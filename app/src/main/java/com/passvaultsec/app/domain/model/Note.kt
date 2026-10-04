package com.passvaultsec.app.domain.model

import java.util.UUID

/**
 * Modelo de dominio para una nota en PassVaultSec.
 */
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String = "",
    val ownerEmail: String = "",
    val title: String = "",
    val content: String = "",
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val color: Long = 0xFFFFFFFF, // Color por defecto (blanco)
    val textColor: Long? = null, // Color de texto personalizado (null = contraste automático)
    val imageUris: List<String> = emptyList(), // Rutas locales de fotos / GIFs adjuntos
    val urlPreviews: List<UrlPreview> = emptyList(), // Vistas previas de enlaces
    val location: NoteLocation? = null, // Ubicación geográfica real y Google Maps
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val labels: List<String> = emptyList(),
    val collaborators: Map<String, Collaborator> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Lista de correos de colaboradores para consultas indexadas en Firestore.
     */
    val collaboratorEmails: List<String>
        get() = collaborators.keys.map { it.lowercase() }
    /**
     * Comprueba si un usuario dado tiene permisos de edición sobre la nota.
     */
    fun canEdit(userEmail: String, userId: String): Boolean {
        if (ownerId == userId || ownerEmail.equals(userEmail, ignoreCase = true)) return true
        val collaborator = collaborators[userEmail.lowercase()] ?: return false
        return collaborator.role == CollaboratorRole.EDITOR
    }

    /**
     * Comprueba si un usuario dado tiene al menos permisos de lectura.
     */
    fun canRead(userEmail: String, userId: String): Boolean {
        if (ownerId == userId || ownerEmail.equals(userEmail, ignoreCase = true)) return true
        return collaborators.containsKey(userEmail.lowercase())
    }
}
