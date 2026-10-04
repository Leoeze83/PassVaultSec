package com.passvaultsec.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.passvaultsec.app.domain.model.ChecklistItem
import com.passvaultsec.app.domain.model.Collaborator
import com.passvaultsec.app.domain.model.CollaboratorRole
import com.passvaultsec.app.domain.model.Note
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Servicio para sincronización en tiempo real y gestión colaborativa con Cloud Firestore.
 */
class FirestoreService {

    private val firestore = FirebaseFirestore.getInstance()
    private val notesCollection = firestore.collection("notes")

    /**
     * Escucha en tiempo real todas las notas donde el usuario es creador.
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
                    close(error)
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
     * Añade o actualiza un colaborador con permisos sobre la nota.
     */
    suspend fun updateCollaborator(
        noteId: String,
        email: String,
        role: CollaboratorRole
    ): Result<Unit> = runCatching {
        val key = email.lowercase().replace(".", "_dot_")
        val collaboratorData = mapOf(
            "email" to email.lowercase(),
            "role" to role.value,
            "addedAt" to System.currentTimeMillis()
        )
        notesCollection.document(noteId).update("collaborators.$key", collaboratorData).await()
    }

    /**
     * Remueve a un colaborador de la nota.
     */
    suspend fun removeCollaborator(noteId: String, email: String): Result<Unit> = runCatching {
        val key = email.lowercase().replace(".", "_dot_")
        notesCollection.document(noteId).update("collaborators.$key", null).await()
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

            Note(
                id = id,
                ownerId = data["ownerId"] as? String ?: "",
                ownerEmail = data["ownerEmail"] as? String ?: "",
                title = data["title"] as? String ?: "",
                content = data["content"] as? String ?: "",
                isChecklist = data["isChecklist"] as? Boolean ?: false,
                checklistItems = checklistItems,
                color = (data["color"] as? Number)?.toLong() ?: 0xFFFFFFFF,
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

        return mapOf(
            "id" to note.id,
            "ownerId" to note.ownerId,
            "ownerEmail" to note.ownerEmail,
            "title" to note.title,
            "content" to note.content,
            "isChecklist" to note.isChecklist,
            "checklistItems" to note.checklistItems.map {
                mapOf("id" to it.id, "text" to it.text, "isDone" to it.isDone)
            },
            "color" to note.color,
            "isPinned" to note.isPinned,
            "isLocked" to note.isLocked,
            "isArchived" to note.isArchived,
            "isDeleted" to note.isDeleted,
            "labels" to note.labels,
            "collaborators" to serializedCollaborators,
            "createdAt" to note.createdAt,
            "updatedAt" to note.updatedAt
        )
    }
}
