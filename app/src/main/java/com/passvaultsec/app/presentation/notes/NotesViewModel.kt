package com.passvaultsec.app.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.passvaultsec.app.domain.model.Note
import com.passvaultsec.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NotesFilter {
    ALL,
    PINNED,
    LOCKED,
    ARCHIVED
}

data class NotesUiState(
    val searchQuery: String = "",
    val activeFilter: NotesFilter = NotesFilter.ALL,
    val isGridLayout: Boolean = true,
    val errorMessage: String? = null
)

class NotesViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState

    val notes: StateFlow<List<Note>> = repository.getNotes()
        .combine(_uiState) { rawNotes, state ->
            rawNotes.filter { note ->
                // Filtro por estado
                val matchesFilter = when (state.activeFilter) {
                    NotesFilter.ALL -> !note.isArchived
                    NotesFilter.PINNED -> note.isPinned && !note.isArchived
                    NotesFilter.LOCKED -> note.isLocked && !note.isArchived
                    NotesFilter.ARCHIVED -> note.isArchived
                }

                // Filtro por búsqueda
                val matchesQuery = if (state.searchQuery.isBlank()) {
                    true
                } else {
                    note.title.contains(state.searchQuery, ignoreCase = true) ||
                            (!note.isLocked && note.content.contains(state.searchQuery, ignoreCase = true)) ||
                            note.labels.any { it.contains(state.searchQuery, ignoreCase = true) }
                }

                matchesFilter && matchesQuery
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onFilterChanged(filter: NotesFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    fun toggleLayout() {
        _uiState.value = _uiState.value.copy(isGridLayout = !_uiState.value.isGridLayout)
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.togglePin(note.id)
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            repository.toggleArchive(note.id)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(repository) as T
        }
    }
}
