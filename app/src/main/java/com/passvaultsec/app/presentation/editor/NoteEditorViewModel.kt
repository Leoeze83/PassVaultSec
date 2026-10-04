package com.passvaultsec.app.presentation.editor

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.passvaultsec.app.core.auth.GoogleAuthManager
import com.passvaultsec.app.core.security.BiometricAuthManager
import com.passvaultsec.app.domain.model.ChecklistItem
import com.passvaultsec.app.domain.model.CollaboratorRole
import com.passvaultsec.app.domain.model.Note
import com.passvaultsec.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val note: Note = Note(),
    val isNewNote: Boolean = true,
    val canEdit: Boolean = true,
    val isCollaboratorDialogOpen: Boolean = false,
    val isColorPickerOpen: Boolean = false,
    val isTextColorPickerOpen: Boolean = false,
    val isEmojiPickerOpen: Boolean = false,
    val isInsertUrlDialogOpen: Boolean = false,
    val isLoadingUrlPreview: Boolean = false,
    val isLocationLoading: Boolean = false,
    val errorMessage: String? = null
)

class NoteEditorViewModel(
    private val noteId: String?,
    private val startAsChecklist: Boolean,
    private val repository: NoteRepository,
    private val authManager: GoogleAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState

    init {
        loadNote()
    }

    private fun loadNote() {
        val currentUserEmail = authManager.getCurrentUserEmail()
        val currentUserId = authManager.getCurrentUserId()

        if (noteId != null) {
            viewModelScope.launch {
                val existingNote = repository.getNoteById(noteId)
                if (existingNote != null) {
                    val canEdit = existingNote.canEdit(currentUserEmail, currentUserId)
                    _uiState.value = _uiState.value.copy(
                        note = existingNote,
                        isNewNote = false,
                        canEdit = canEdit
                    )
                }
            }
        } else {
            val initialNote = Note(
                ownerId = currentUserId,
                ownerEmail = currentUserEmail,
                isChecklist = startAsChecklist,
                checklistItems = if (startAsChecklist) listOf(ChecklistItem(text = "")) else emptyList()
            )
            _uiState.value = _uiState.value.copy(
                note = initialNote,
                isNewNote = true,
                canEdit = true
            )
        }
    }

    fun onTitleChange(newTitle: String) {
        if (!_uiState.value.canEdit) return
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(title = newTitle)
        )
    }

    fun onContentChange(newContent: String) {
        if (!_uiState.value.canEdit) return
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(content = newContent)
        )
    }

    fun onColorChange(colorLong: Long) {
        if (!_uiState.value.canEdit) return
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(color = colorLong)
        )
    }

    fun onTextColorChange(textColorLong: Long?) {
        if (!_uiState.value.canEdit) return
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(textColor = textColorLong)
        )
    }

    fun onAddImage(imagePath: String) {
        if (!_uiState.value.canEdit) return
        val currentImages = _uiState.value.note.imageUris.toMutableList()
        currentImages.add(imagePath)
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(imageUris = currentImages)
        )
        saveNote()
    }

    fun onRemoveImage(index: Int) {
        if (!_uiState.value.canEdit) return
        val currentImages = _uiState.value.note.imageUris.toMutableList()
        if (index in currentImages.indices) {
            currentImages.removeAt(index)
            _uiState.value = _uiState.value.copy(
                note = _uiState.value.note.copy(imageUris = currentImages)
            )
            saveNote()
        }
    }

    fun onInsertEmoji(emoji: String) {
        if (!_uiState.value.canEdit) return
        val currentNote = _uiState.value.note
        if (currentNote.isChecklist && currentNote.checklistItems.isNotEmpty()) {
            val lastIndex = currentNote.checklistItems.size - 1
            val lastItem = currentNote.checklistItems[lastIndex]
            val updatedItem = lastItem.copy(text = lastItem.text + emoji)
            onChecklistItemChange(lastIndex, updatedItem)
        } else {
            val updatedContent = currentNote.content + emoji
            onContentChange(updatedContent)
        }
    }

    fun onAddUrlPreview(url: String) {
        if (!_uiState.value.canEdit) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingUrlPreview = true, isInsertUrlDialogOpen = false)
            val preview = com.passvaultsec.app.core.ui.util.UrlMetadataExtractor.fetchUrlPreview(url)
            if (preview != null) {
                val currentPreviews = _uiState.value.note.urlPreviews.toMutableList()
                if (!currentPreviews.any { it.url.equals(url, ignoreCase = true) }) {
                    currentPreviews.add(preview)
                    _uiState.value = _uiState.value.copy(
                        note = _uiState.value.note.copy(urlPreviews = currentPreviews)
                    )
                    saveNote()
                }
            }
            _uiState.value = _uiState.value.copy(isLoadingUrlPreview = false)
        }
    }

    fun onRemoveUrlPreview(index: Int) {
        if (!_uiState.value.canEdit) return
        val currentPreviews = _uiState.value.note.urlPreviews.toMutableList()
        if (index in currentPreviews.indices) {
            currentPreviews.removeAt(index)
            _uiState.value = _uiState.value.copy(
                note = _uiState.value.note.copy(urlPreviews = currentPreviews)
            )
            saveNote()
        }
    }

    fun onTogglePin() {
        val updated = !_uiState.value.note.isPinned
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(isPinned = updated)
        )
    }

    fun onToggleChecklist() {
        if (!_uiState.value.canEdit) return
        val current = _uiState.value.note
        val willBeChecklist = !current.isChecklist

        val newItems = if (willBeChecklist && current.checklistItems.isEmpty()) {
            val lines = current.content.lines().filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                lines.map { ChecklistItem(text = it) }
            } else {
                listOf(ChecklistItem(text = ""))
            }
        } else {
            current.checklistItems
        }

        val newContent = if (!willBeChecklist) {
            current.checklistItems.joinToString("\n") { it.text }
        } else {
            current.content
        }

        _uiState.value = _uiState.value.copy(
            note = current.copy(
                isChecklist = willBeChecklist,
                checklistItems = newItems,
                content = newContent
            )
        )
    }

    fun onChecklistItemChange(index: Int, item: ChecklistItem) {
        if (!_uiState.value.canEdit) return
        val items = _uiState.value.note.checklistItems.toMutableList()
        if (index in items.indices) {
            items[index] = item
            _uiState.value = _uiState.value.copy(
                note = _uiState.value.note.copy(checklistItems = items)
            )
        }
    }

    fun onChecklistItemDelete(index: Int) {
        if (!_uiState.value.canEdit) return
        val items = _uiState.value.note.checklistItems.toMutableList()
        if (index in items.indices) {
            items.removeAt(index)
            _uiState.value = _uiState.value.copy(
                note = _uiState.value.note.copy(checklistItems = items)
            )
        }
    }

    fun onAddChecklistItem() {
        if (!_uiState.value.canEdit) return
        val items = _uiState.value.note.checklistItems.toMutableList()
        items.add(ChecklistItem(text = ""))
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(checklistItems = items)
        )
    }

    fun onToggleLock(
        activity: FragmentActivity,
        biometricAuthManager: BiometricAuthManager,
        onSuccess: () -> Unit
    ) {
        val willLock = !_uiState.value.note.isLocked
        val promptTitle = if (willLock) "Proteger Nota con Biometría/PIN" else "Desbloquear Nota"

        biometricAuthManager.authenticate(
            activity = activity,
            title = promptTitle,
            subtitle = "Confirma tu identidad para modificar la protección",
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    note = _uiState.value.note.copy(isLocked = willLock)
                )
                onSuccess()
            },
            onError = { _, err ->
                _uiState.value = _uiState.value.copy(errorMessage = err.toString())
            }
        )
    }

    fun setCollaboratorDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isCollaboratorDialogOpen = isOpen)
    }

    fun setColorPickerOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(
            isColorPickerOpen = isOpen,
            isTextColorPickerOpen = if (isOpen) false else _uiState.value.isTextColorPickerOpen,
            isEmojiPickerOpen = false
        )
    }

    fun setTextColorPickerOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(
            isTextColorPickerOpen = isOpen,
            isColorPickerOpen = if (isOpen) false else _uiState.value.isColorPickerOpen,
            isEmojiPickerOpen = false
        )
    }

    fun setEmojiPickerOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(
            isEmojiPickerOpen = isOpen,
            isColorPickerOpen = false,
            isTextColorPickerOpen = false
        )
    }

    fun setInsertUrlDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isInsertUrlDialogOpen = isOpen)
    }

    fun addCollaborator(email: String, role: CollaboratorRole) {
        val currentCollaborators = _uiState.value.note.collaborators.toMutableMap()
        currentCollaborators[email.lowercase()] = com.passvaultsec.app.domain.model.Collaborator(
            email = email.lowercase(),
            role = role
        )
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(collaborators = currentCollaborators)
        )
        saveNote()
    }

    fun removeCollaborator(email: String) {
        val currentCollaborators = _uiState.value.note.collaborators.toMutableMap()
        currentCollaborators.remove(email.lowercase())
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(collaborators = currentCollaborators)
        )
        saveNote()
    }

    fun setLocation(location: com.passvaultsec.app.domain.model.NoteLocation?) {
        if (!_uiState.value.canEdit) return
        _uiState.value = _uiState.value.copy(
            note = _uiState.value.note.copy(location = location),
            isLocationLoading = false
        )
    }

    fun setLocationLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLocationLoading = isLoading)
    }

    fun saveNote() {
        val currentNote = _uiState.value.note
        // Si la nota está totalmente vacía y es nueva, no guardamos basura
        if (currentNote.title.isBlank() && currentNote.content.isBlank() && currentNote.checklistItems.isEmpty() && currentNote.imageUris.isEmpty() && currentNote.urlPreviews.isEmpty() && currentNote.location == null) {
            return
        }

        viewModelScope.launch {
            repository.saveNote(currentNote)
        }
    }

    fun deleteNote(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNote(_uiState.value.note.id)
            onComplete()
        }
    }

    class Factory(
        private val noteId: String?,
        private val startAsChecklist: Boolean,
        private val repository: NoteRepository,
        private val authManager: GoogleAuthManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteEditorViewModel(noteId, startAsChecklist, repository, authManager) as T
        }
    }
}
