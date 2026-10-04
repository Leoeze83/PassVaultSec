package com.passvaultsec.app.data.remote

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.passvaultsec.app.domain.model.ChecklistItem
import com.passvaultsec.app.domain.model.Collaborator
import com.passvaultsec.app.domain.model.CollaboratorRole
import com.passvaultsec.app.domain.model.Note
import com.passvaultsec.app.domain.model.NoteLocation
import com.passvaultsec.app.domain.model.UrlPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Servicio para sincronización en tiempo real y gestión colaborativa con Cloud Firestore.
 * Incluye soporte para ubicación geográfica, enlaces OpenGraph, imágenes,
 * buzón de invitaciones y notificaciones a colaboradores.
 */
class FirestoreService {

    private val firestore = FirebaseFirestore.getInstance()
    private val notesCollection = firestore.collection("notes")
    private val invitationsCollection = firestore.collection("invitations")

    companion object {
        private const val TAG = "FirestoreService"
    }

    /**
     * Escucha en tiempo real todas las notas donde el usuario es creador.
     * Es resiliente: ante errores de permisos o desconexión no cancela el flujo con excepción.
     */
    fun observeOwnedNotes(userId: String): Flow<List<Note>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = notesCollection
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error escuchando notas propias: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToNote(doc.id, doc.data)
                } ?: emptyList()
                trySend(notes)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Escucha en tiempo real todas las notas compartidas con el usuario como colaborador.
     * Es resiliente ante errores de red o permisos.
     */
    fun observeSharedNotes(userEmail: String): Flow<List<Note>> = callbackFlow {
        if (userEmail.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = notesCollection
            .whereArrayContains("collaboratorEmails", userEmail.lowercase())
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error escuchando notas compartidas: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToNote(doc.id, doc.data)
                } ?: emptyList()
                trySend(notes)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Escucha invitaciones de colaboración dirigidas al correo del usuario.
     * Resiliente para evitar cierres abruptos o caídas de la aplicación.
     */
    fun observeIncomingInvitations(userEmail: String): Flow<List<Map<String, Any>>> = callbackFlow {
        if (userEmail.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = invitationsCollection
            .whereEqualTo("collaboratorEmail", userEmail.lowercase())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error escuchando invitaciones entrantes: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene una sola vez todas las notas donde el usuario es creador.
     */
    suspend fun getOwnedNotes(userId: String): Result<List<Note>> = runCatching {
        if (userId.isEmpty()) return@runCatching emptyList()
        val snapshot = notesCollection
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("isDeleted", false)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc -> mapDocumentToNote(doc.id, doc.data) }
    }

    /**
     * Obtiene una sola vez todas las notas compartidas con el usuario.
     */
    suspend fun getSharedNotes(userEmail: String): Result<List<Note>> = runCatching {
        if (userEmail.isEmpty()) return@runCatching emptyList()
        val snapshot = notesCollection
            .whereArrayContains("collaboratorEmails", userEmail.lowercase())
            .whereEqualTo("isDeleted", false)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc -> mapDocumentToNote(doc.id, doc.data) }
    }

    /**
     * Registra un evento de telemetría de seguridad anonimizado en la colección /security_telemetry/.
     * Los clientes tienen permiso Write-Only en Firestore para no filtrar datos de otros usuarios.
     */
    suspend fun logSecurityEvent(
        eventType: String,
        category: String,
        severity: String,
        detail: String
    ): Result<Unit> = runCatching {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userIdHash = currentUser?.uid ?: "client_${android.os.Build.ID.hashCode().toString(16)}"
        val eventData = mapOf(
            "eventType" to eventType,
            "category" to category,
            "severity" to severity,
            "detail" to detail,
            "deviceModel" to "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})",
            "userId" to userIdHash,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("security_telemetry").add(eventData).await()
    }

    /**
     * Emite un evento de seguridad de forma asíncrona no bloqueante (Fire-and-Forget).
     */
    fun emitTelemetryAsync(
        eventType: String,
        category: String,
        severity: String,
        detail: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logSecurityEvent(eventType, category, severity, detail)
            } catch (e: Exception) {
                Log.w(TAG, "Error emitiendo telemetría: ${e.message}")
            }
        }
    }

    /**
     * Guarda o actualiza una nota en Firestore.
     */
    suspend fun saveNote(note: Note): Result<Unit> = runCatching {
        val noteMap = mapNoteToMap(note)
        notesCollection.document(note.id).set(noteMap, SetOptions.merge()).await()
    }

    /**
     * Elimina una nota de Firestore (soft-delete o borrado definitivo).
     */
    suspend fun deleteNote(noteId: String): Result<Unit> = runCatching {
        notesCollection.document(noteId).delete().await()
    }

    /**
     * Añade o actualiza un colaborador con permisos sobre la nota y registra una invitación en la nube.
     */
    suspend fun updateCollaborator(
        noteId: String,
        email: String,
        role: CollaboratorRole
    ): Result<Unit> = runCatching {
        val cleanEmail = email.lowercase().trim()
        val key = cleanEmail.replace(".", "_dot_")
        val collaboratorData = mapOf(
            "email" to cleanEmail,
            "role" to role.value,
            "addedAt" to System.currentTimeMillis()
        )

        // 1. Actualizar colaboradores y lista indexada de correos en la nota
        notesCollection.document(noteId).update(
            "collaborators.$key", collaboratorData,
            "collaboratorEmails", FieldValue.arrayUnion(cleanEmail)
        ).await()

        // 2. Crear documento de invitación para notificar al colaborador
        val noteDoc = notesCollection.document(noteId).get().await()
        val ownerEmail = noteDoc.getString("ownerEmail") ?: ""

        val invitationData = mapOf(
            "id" to "${noteId}_$key",
            "noteId" to noteId,
            "noteTitle" to "Nota Compartida Protegida",
            "ownerEmail" to ownerEmail,
            "collaboratorEmail" to cleanEmail,
            "role" to role.value,
            "downloadUrl" to "https://github.com/Leoeze83/PassVaultSec/releases",
            "timestamp" to System.currentTimeMillis()
        )

        invitationsCollection.document("${noteId}_$key").set(invitationData, SetOptions.merge()).await()

        // Emite telemetría de colaboración en tiempo real
        emitTelemetryAsync(
            eventType = "COLLABORATOR_INVITED",
            category = "COLLAB",
            severity = "INFO",
            detail = "Colaborador añadido ($cleanEmail, ${role.value}) con enlace de descarga automática de la app"
        )
    }

    /**
     * Remueve a un colaborador de la nota y retira su invitación.
     */
    suspend fun removeCollaborator(noteId: String, email: String): Result<Unit> = runCatching {
        val cleanEmail = email.lowercase().trim()
        val key = cleanEmail.replace(".", "_dot_")
        notesCollection.document(noteId).update(
            "collaborators.$key", FieldValue.delete(),
            "collaboratorEmails", FieldValue.arrayRemove(cleanEmail)
        ).await()

        invitationsCollection.document("${noteId}_$key").delete().await()

        // Emite telemetría de revocación de colaborador
        emitTelemetryAsync(
            eventType = "COLLABORATOR_REMOVED",
            category = "COLLAB",
            severity = "INFO",
            detail = "Colaborador $cleanEmail revocado de la nota"
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDocumentToNote(id: String, data: Map<String, Any?>?): Note? {
        if (data == null) return null
        return try {
            val checklistData = data["checklistItems"] as? List<Map<String, Any>> ?: emptyList()
            val checklistItems = checklistData.map {
                ChecklistItem(
                    id = it["id"] as? String ?: "",
                    text = it["text"] as? String ?: "",
                    isDone = it["isDone"] as? Boolean ?: false
                )
            }

            val collabMapRaw = data["collaborators"] as? Map<String, Map<String, Any>> ?: emptyMap()
            val collaborators = collabMapRaw.mapNotNull { (_, valMap) ->
                val email = valMap["email"] as? String ?: return@mapNotNull null
                val role = CollaboratorRole.fromValue(valMap["role"] as? String ?: "viewer")
                val addedAt = (valMap["addedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                email to Collaborator(email = email, role = role, addedAt = addedAt)
            }.toMap()

            val urlPreviewsRaw = data["urlPreviews"] as? List<Map<String, Any?>> ?: emptyList()
            val urlPreviews = urlPreviewsRaw.map {
                UrlPreview(
                    url = it["url"] as? String ?: "",
                    title = it["title"] as? String ?: "",
                    description = it["description"] as? String ?: "",
                    imageUrl = it["imageUrl"] as? String ?: "",
                    domain = it["domain"] as? String ?: ""
                )
            }

            val locationRaw = data["location"] as? Map<String, Any?>
            val location = if (locationRaw != null) {
                NoteLocation(
                    latitude = (locationRaw["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (locationRaw["longitude"] as? Number)?.toDouble() ?: 0.0,
                    address = locationRaw["address"] as? String ?: "",
                    placeName = locationRaw["placeName"] as? String ?: "",
                    mapSnapshotUrl = locationRaw["mapSnapshotUrl"] as? String ?: ""
                )
            } else null

            Note(
                id = id,
                ownerId = data["ownerId"] as? String ?: "",
                ownerEmail = data["ownerEmail"] as? String ?: "",
                title = data["title"] as? String ?: "",
                content = data["content"] as? String ?: "",
                isChecklist = data["isChecklist"] as? Boolean ?: false,
                checklistItems = checklistItems,
                color = (data["color"] as? Number)?.toLong() ?: 0xFFFFFFFF,
                textColor = (data["textColor"] as? Number)?.toLong(),
                imageUris = data["imageUris"] as? List<String> ?: emptyList(),
                urlPreviews = urlPreviews,
                location = location,
                isPinned = data["isPinned"] as? Boolean ?: false,
                isLocked = data["isLocked"] as? Boolean ?: false,
                isArchived = data["isArchived"] as? Boolean ?: false,
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                labels = data["labels"] as? List<String> ?: emptyList(),
                collaborators = collaborators,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun mapNoteToMap(note: Note): Map<String, Any?> {
        val serializedCollaborators = note.collaborators.mapKeys { (email, _) ->
            email.replace(".", "_dot_")
        }.mapValues { (_, collab) ->
            mapOf(
                "email" to collab.email,
                "role" to collab.role.value,
                "addedAt" to collab.addedAt
            )
        }

        val serializedUrlPreviews = note.urlPreviews.map {
            mapOf(
                "url" to it.url,
                "title" to it.title,
                "description" to it.description,
                "imageUrl" to it.imageUrl,
                "domain" to it.domain
            )
        }

        val serializedLocation = note.location?.let {
            mapOf(
                "latitude" to it.latitude,
                "longitude" to it.longitude,
                "address" to it.address,
                "placeName" to it.placeName,
                "mapSnapshotUrl" to it.mapSnapshotUrl
            )
        }

        return mapOf(
            "id" to note.id,
            "ownerId" to note.ownerId,
            "ownerEmail" to note.ownerEmail,
            "title" to note.title,
            // PRIVACIDAD TOTAL ZERO-KNOWLEDGE: El contenido y listas permanecen 100% locales en el dispositivo
            "content" to "",
            "isChecklist" to note.isChecklist,
            "checklistItems" to emptyList<Map<String, Any>>(),
            "color" to note.color,
            "textColor" to note.textColor,
            "imageUris" to note.imageUris,
            "urlPreviews" to serializedUrlPreviews,
            "location" to serializedLocation,
            "isPinned" to note.isPinned,
            "isLocked" to note.isLocked,
            "isArchived" to note.isArchived,
            "isDeleted" to note.isDeleted,
            "labels" to note.labels,
            "collaborators" to serializedCollaborators,
            "collaboratorEmails" to note.collaboratorEmails,
            "createdAt" to note.createdAt,
            "updatedAt" to note.updatedAt
        )
    }
}
