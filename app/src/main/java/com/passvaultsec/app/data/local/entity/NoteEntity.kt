package com.passvaultsec.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.passvaultsec.app.domain.model.ChecklistItem
import com.passvaultsec.app.domain.model.Collaborator
import com.passvaultsec.app.domain.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val ownerEmail: String,
    val title: String,
    val content: String,
    val isChecklist: Boolean,
    val checklistItems: List<ChecklistItem>,
    val color: Long,
    val textColor: Long? = null,
    val imageUris: List<String> = emptyList(),
    val urlPreviews: List<com.passvaultsec.app.domain.model.UrlPreview> = emptyList(),
    val isPinned: Boolean,
    val isLocked: Boolean,
    val isArchived: Boolean,
    val isDeleted: Boolean,
    val labels: List<String>,
    val collaborators: Map<String, Collaborator>,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Note {
        return Note(
            id = id,
            ownerId = ownerId,
            ownerEmail = ownerEmail,
            title = title,
            content = content,
            isChecklist = isChecklist,
            checklistItems = checklistItems,
            color = color,
            textColor = textColor,
            imageUris = imageUris,
            urlPreviews = urlPreviews,
            isPinned = isPinned,
            isLocked = isLocked,
            isArchived = isArchived,
            isDeleted = isDeleted,
            labels = labels,
            collaborators = collaborators,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
                ownerId = note.ownerId,
                ownerEmail = note.ownerEmail,
                title = note.title,
                content = note.content,
                isChecklist = note.isChecklist,
                checklistItems = note.checklistItems,
                color = note.color,
                textColor = note.textColor,
                imageUris = note.imageUris,
                urlPreviews = note.urlPreviews,
                isPinned = note.isPinned,
                isLocked = note.isLocked,
                isArchived = note.isArchived,
                isDeleted = note.isDeleted,
                labels = note.labels,
                collaborators = note.collaborators,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt
            )
        }
    }
}
