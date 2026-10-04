package com.passvaultsec.app.domain.model

enum class CollaboratorRole(val value: String) {
    VIEWER("viewer"),
    EDITOR("editor");

    companion object {
        fun fromValue(value: String): CollaboratorRole {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: VIEWER
        }
    }
}

/**
 * Representa a un usuario con quien se comparte la nota.
 */
data class Collaborator(
    val email: String,
    val role: CollaboratorRole = CollaboratorRole.EDITOR,
    val addedAt: Long = System.currentTimeMillis()
)
