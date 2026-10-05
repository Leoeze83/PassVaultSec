package com.passvaultsec.app.data.repository

import android.util.Log
import com.passvaultsec.app.core.auth.GoogleAuthManager
import com.passvaultsec.app.core.security.CryptoManager
import com.passvaultsec.app.data.local.dao.NoteDao
import com.passvaultsec.app.data.local.entity.NoteEntity
import com.passvaultsec.app.data.remote.FirestoreService
import com.passvaultsec.app.domain.model.CollaboratorRole
import com.passvaultsec.app.domain.model.Note
import com.passvaultsec.app.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Implementación del repositorio de notas con persistencia local en Room,
 * cifrado de seguridad AES-256 en Keystore y sincronización remota bidireccional
 * en tiempo real con Cloud Firestore para notas propias y compartidas con colaboradores.
 */
class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val firestoreService: FirestoreService,
    private val cryptoManager: CryptoManager,
    private val authManager: GoogleAuthManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NoteRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var realtimeSyncJob: Job? = null

    init {
        // Iniciar sincronización en tiempo real reactiva al estado de autenticación
        repositoryScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null) {
                    val uid = user.uid
                    val email = user.email.orEmpty().trim().lowercase()
                    startRealtimeSync(uid, email)
                } else {
                    stopRealtimeSync()
                }
            }
        }
    }

    /**
     * Inicia los observadores en tiempo real de Firestore para notas propias y compartidas.
     */
    private fun startRealtimeSync(userId: String, userEmail: String) {
        stopRealtimeSync()
        realtimeSyncJob = repositoryScope.launch {
            // 1. Escuchar notas propias en tiempo real
            launch {
                firestoreService.observeOwnedNotes(userId)
                    .catch { e -> Log.w("NoteRepositoryImpl", "Error en stream de notas propias: ${e.message}") }
                    .collect { notes ->
                        applyRemoteNotesToLocal(notes)
                    }
            }

            // 2. Escuchar notas compartidas con el usuario como colaborador en tiempo real
            if (userEmail.isNotEmpty()) {
                launch {
                    firestoreService.observeSharedNotes(userEmail)
                        .catch { e -> Log.w("NoteRepositoryImpl", "Error en stream de notas compartidas: ${e.message}") }
                        .collect { notes ->
                            applyRemoteNotesToLocal(notes)
                        }
                }
            }
        }
    }

    private fun stopRealtimeSync() {
        realtimeSyncJob?.cancel()
        realtimeSyncJob = null
    }

    /**
     * Aplica notas remotas a la base de datos local Room resolviendo versiones por timestamp.
     */
    private suspend fun applyRemoteNotesToLocal(remoteNotes: List<Note>) = withContext(ioDispatcher) {
        for (remoteNote in remoteNotes) {
            val existing = noteDao.getNoteById(remoteNote.id)
            if (existing == null) {
                if (!remoteNote.isDeleted) {
                    noteDao.upsertNote(NoteEntity.fromDomain(remoteNote))
                }
            } else {
                if (remoteNote.isDeleted) {
                    noteDao.softDeleteNote(remoteNote.id, remoteNote.updatedAt)
                } else if (remoteNote.updatedAt >= existing.updatedAt) {
                    val mergedNote = if (existing.isLocked && !remoteNote.isLocked) {
                        remoteNote.copy(isLocked = true)
                    } else {
                        remoteNote
                    }
                    noteDao.upsertNote(NoteEntity.fromDomain(mergedNote))
                } else {
                    // Local es más reciente: sincronizar hacia Firestore
                    if (authManager.isUserSignedIn()) {
                        firestoreService.saveNote(existing.toDomain())
                    }
                }
            }
        }
    }

    override fun getNotes(): Flow<List<Note>> {
        return noteDao.getAllActiveNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(id: String): Note? = withContext(ioDispatcher) {
        noteDao.getNoteById(id)?.toDomain()
    }

    override fun observeNoteById(id: String): Flow<Note?> {
        return noteDao.observeNoteById(id).map { it?.toDomain() }
    }

    override suspend fun saveNote(note: Note): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val user = authManager.currentUser.value
            val currentUserId = user?.uid.orEmpty()
            val currentUserEmail = user?.email.orEmpty().trim().lowercase()

            val existing = noteDao.getNoteById(note.id)
            val baseTimestamp = maxOf(note.updatedAt, existing?.updatedAt ?: 0L)
            val now = maxOf(System.currentTimeMillis(), baseTimestamp + 1)
            val resolvedOwnerId = if (note.ownerId.isNotEmpty()) note.ownerId else (existing?.ownerId?.ifEmpty { currentUserId } ?: currentUserId)
            val resolvedOwnerEmail = if (note.ownerEmail.isNotEmpty()) note.ownerEmail else (existing?.ownerEmail?.ifEmpty { currentUserEmail } ?: currentUserEmail)

            val preparedNote = note.copy(
                ownerId = resolvedOwnerId,
                ownerEmail = resolvedOwnerEmail,
                updatedAt = now
            )

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
            val now = System.currentTimeMillis()
            val existing = noteDao.getNoteById(id)
            noteDao.softDeleteNote(id, now)

            if (authManager.isUserSignedIn()) {
                if (existing != null) {
                    val domainNote = existing.toDomain().copy(isDeleted = true, updatedAt = now)
                    firestoreService.saveNote(domainNote)
                } else {
                    firestoreService.deleteNote(id)
                }
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
            val now = System.currentTimeMillis()
            noteDao.updatePinStatus(id, updated, now)
            if (authManager.isUserSignedIn()) {
                val domainNote = existing.toDomain().copy(isPinned = updated, updatedAt = now)
                firestoreService.saveNote(domainNote)
            }
        }
    }

    override suspend fun toggleLock(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = noteDao.getNoteById(id) ?: return@runCatching
            val newLockState = !existing.isLocked
            val now = System.currentTimeMillis()

            val newContent = if (newLockState) {
                cryptoManager.encryptString(existing.content)
            } else {
                try {
                    cryptoManager.decryptString(existing.content)
                } catch (e: Exception) {
                    existing.content
                }
            }

            val updatedEntity = existing.copy(
                isLocked = newLockState,
                content = newContent,
                updatedAt = now
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
            val now = System.currentTimeMillis()
            noteDao.updateArchiveStatus(id, updated, now)
            if (authManager.isUserSignedIn()) {
                val domainNote = existing.toDomain().copy(isArchived = updated, updatedAt = now)
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
            val cleanEmail = email.lowercase().trim()
            val existing = noteDao.getNoteById(noteId) ?: return@runCatching
            val updatedCollaborators = existing.collaborators.toMutableMap()
            val now = System.currentTimeMillis()
            updatedCollaborators[cleanEmail] = com.passvaultsec.app.domain.model.Collaborator(
                email = cleanEmail,
                role = role,
                addedAt = now
            )

            val updatedEntity = existing.copy(
                collaborators = updatedCollaborators,
                updatedAt = now
            )

            noteDao.upsertNote(updatedEntity)
            if (authManager.isUserSignedIn()) {
                firestoreService.updateCollaborator(noteId, cleanEmail, role)
                firestoreService.saveNote(updatedEntity.toDomain())
            }
        }
    }

    override suspend fun removeCollaborator(noteId: String, email: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.lowercase().trim()
            val existing = noteDao.getNoteById(noteId) ?: return@runCatching
            val updatedCollaborators = existing.collaborators.toMutableMap()
            val now = System.currentTimeMillis()
            updatedCollaborators.remove(cleanEmail)

            val updatedEntity = existing.copy(
                collaborators = updatedCollaborators,
                updatedAt = now
            )

            noteDao.upsertNote(updatedEntity)
            if (authManager.isUserSignedIn()) {
                firestoreService.removeCollaborator(noteId, cleanEmail)
                firestoreService.saveNote(updatedEntity.toDomain())
            }
        }
    }

    override suspend fun syncNotes(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val userId = authManager.getCurrentUserId()
            val userEmail = authManager.getCurrentUserEmail().lowercase().trim()
            if (userId.isEmpty() && userEmail.isEmpty()) return@runCatching

            if (userId.isNotEmpty()) {
                val ownedNotes = firestoreService.getOwnedNotes(userId).getOrDefault(emptyList())
                applyRemoteNotesToLocal(ownedNotes)
            }

            if (userEmail.isNotEmpty()) {
                val sharedNotes = firestoreService.getSharedNotes(userEmail).getOrDefault(emptyList())
                applyRemoteNotesToLocal(sharedNotes)
            }
        }
    }
}
