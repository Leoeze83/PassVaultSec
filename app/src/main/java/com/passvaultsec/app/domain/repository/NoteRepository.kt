package com.passvaultsec.app.domain.repository

import com.passvaultsec.app.domain.model.CollaboratorRole
import com.passvaultsec.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para operaciones locales y remotas con notas.
 */
interface NoteRepository {
    fun getNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: String): Note?
    fun observeNoteById(id: String): Flow<Note?>
    suspend fun saveNote(note: Note): Result<Unit>
    suspend fun deleteNote(id: String): Result<Unit>
    suspend fun togglePin(id: String): Result<Unit>
    suspend fun toggleLock(id: String): Result<Unit>
    suspend fun toggleArchive(id: String): Result<Unit>
    suspend fun addCollaborator(noteId: String, email: String, role: CollaboratorRole): Result<Unit>
    suspend fun removeCollaborator(noteId: String, email: String): Result<Unit>
    suspend fun syncNotes(): Result<Unit>
}
