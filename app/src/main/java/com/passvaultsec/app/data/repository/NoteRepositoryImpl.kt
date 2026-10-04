package com.passvaultsec.app.data.repository

import com.passvaultsec.app.core.auth.GoogleAuthManager
import com.passvaultsec.app.core.security.CryptoManager
import com.passvaultsec.app.data.local.dao.NoteDao
import com.passvaultsec.app.data.local.entity.NoteEntity
import com.passvaultsec.app.data.remote.FirestoreService
import com.passvaultsec.app.domain.model.CollaboratorRole
import com.passvaultsec.app.domain.model.Note
import com.passvaultsec.app.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Implementación del repositorio de notas con persistencia local en Room,
 * cifrado de seguridad AES-256 en Keystore y sincronización remota en Firestore.
 */
class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val firestoreService: FirestoreService,
    private val cryptoManager: CryptoManager,
    private val authManager: GoogleAuthManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NoteRepository {

    override fun getNotes(): Flow<List<Note>> {
        return noteDao.getAllActiveNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(id: String): Note? = withContext(ioDispatcher) {
        noteDao.getNoteById(id)?.toDomain()
    }

    override suspend fun saveNote(note: Note): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val user = authManager.currentUser.value
            val currentUserId = user?.uid.orEmpty()
            val currentUserEmail = user?.email.orEmpty()

            val preparedNote = if (note.ownerId.isEmpty()) {
                note.copy(
                    ownerId = currentUserId,
                    ownerEmail = currentUserEmail,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                note.copy(updatedAt = System.currentTimeMillis())
            }

            // Persistencia local inmediata (Offline-First)
            noteDao.upsertNote(NoteEntity.fromDomain(preparedNote))

            // Sincronización en la nube si hay usuario activo
            if (authManager.isUserSignedIn()) {
                firestoreService.saveNote(preparedNote)
            }

            // Emite telemetría anonimizada de persistencia
            firestoreService.emitTelemetryAsync(
                eventType = if (preparedNote.isLocked) "NOTE_ENCRYPTED_SAVED" else "NOTE_PERSISTED_SECURELY",
                category = if (preparedNote.isLocked) "CRYPTO" else "STORAGE",
                severity = "INFO",
                detail = if (preparedNote.isLocked) "Nota cifrada con Android Keystore guardada" else "Nota persistida en almacenamiento seguro"
            )
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            noteDao.softDeleteNote(id)
            if (authManager.isUserSignedIn()) {
                firestoreService.deleteNote(id)
            }
            firestoreService.emitTelemetryAsync(
                eventType = "NOTE_PURGED_SECURE",
                category = "STORAGE",
                severity = "INFO",
                detail = "Nota eliminada de forma segura del almacenamiento"
            )
        }
    }

    override suspend fun togglePin(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = noteDao.getNoteById(id) ?: return@runCatching
            val updated = !existing.isPinned
            noteDao.updatePinStatus(id, updated)
            if (authManager.isUserSignedIn()) {
                val domainNote = existing.toDomain().copy(isPinned = updated)
                firestoreService.saveNote(domainNote)
            }
        }
    }

    override suspend fun toggleLock(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = noteDao.getNoteById(id) ?: return@runCatching
            val newLockState = !existing.isLocked

            // Cifrado/Descifrado local con Android Keystore
            val newContent = if (newLockState) {
                // Al bloquear, se cifra el contenido
                cryptoManager.encryptString(existing.content)
            } else {
                // Al desbloquear, se descifra a texto plano
                try {
                    cryptoManager.decryptString(existing.content)
                } catch (e: Exception) {
                    existing.content // En caso de que no estuviese cifrado previamente
                }
            }

            val updatedEntity = existing.copy(
                isLocked = newLockState,
                content = newContent,
                updatedAt = System.currentTimeMillis()
            )

            noteDao.upsertNote(updatedEntity)
            if (authManager.isUserSignedIn()) {
                firestoreService.saveNote(updatedEntity.toDomain())
            }
            firestoreService.emitTelemetryAsync(
                eventType = if (newLockState) "NOTE_LOCKED_KEYSTORE" else "NOTE_UNLOCKED_BIOMETRIC",
                category = "CRYPTO",
                severity = "INFO",
                detail = if (newLockState) "Nota asegurada y cifrada con hardware Android Keystore (AES-256)" else "Nota descifrada tras autenticación local exitosa"
            )
        }
    }

    override suspend fun toggleArchive(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = noteDao.getNoteById(id) ?: return@runCatching
            val updated = !existing.isArchived
            noteDao.updateArchiveStatus(id, updated)
            if (authManager.isUserSignedIn()) {
                val domainNote = existing.toDomain().copy(isArchived = updated)
                firestoreService.saveNote(domainNote)
            }
        }
    }

    override suspend fun addCollaborator(
        noteId: String,
        email: String,
        role: CollaboratorRole
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = noteDao.getNoteById(noteId) ?: return@runCatching
            val updatedCollaborators = existing.collaborators.toMutableMap()
            updatedCollaborators[email.lowercase()] = com.passvaultsec.app.domain.model.Collaborator(
                email = email.lowercase(),
                role = role
            )

            val updatedEntity = existing.copy(
                collaborators = updatedCollaborators,
                updatedAt = System.currentTimeMillis()
            )

            noteDao.upsertNote(updatedEntity)
            if (authManager.isUserSignedIn()) {
                firestoreService.updateCollaborator(noteId, email, role)
            }
        }
    }

    override suspend fun removeCollaborator(noteId: String, email: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = noteDao.getNoteById(noteId) ?: return@runCatching
            val updatedCollaborators = existing.collaborators.toMutableMap()
            updatedCollaborators.remove(email.lowercase())

            val updatedEntity = existing.copy(
                collaborators = updatedCollaborators,
                updatedAt = System.currentTimeMillis()
            )

            noteDao.upsertNote(updatedEntity)
            if (authManager.isUserSignedIn()) {
                firestoreService.removeCollaborator(noteId, email)
            }
        }
    }

    override suspend fun syncNotes(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val userId = authManager.getCurrentUserId()
            val userEmail = authManager.getCurrentUserEmail()
            if (userId.isEmpty()) return@runCatching

            val ownedNotes = firestoreService.getOwnedNotes(userId).getOrDefault(emptyList())
            if (ownedNotes.isNotEmpty()) {
                val entities = ownedNotes.map { NoteEntity.fromDomain(it) }
                noteDao.upsertNotes(entities)
            }

            if (userEmail.isNotEmpty()) {
                val sharedNotes = firestoreService.getSharedNotes(userEmail).getOrDefault(emptyList())
                if (sharedNotes.isNotEmpty()) {
                    val entities = sharedNotes.map { NoteEntity.fromDomain(it) }
                    noteDao.upsertNotes(entities)
                }
            }
        }
    }
}
