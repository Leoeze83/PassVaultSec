package com.passvaultsec.app.presentation.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.passvaultsec.app.core.auth.GoogleAuthManager
import com.passvaultsec.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authManager: GoogleAuthManager,
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(user = authManager.currentUser.value))
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
                if (user != null) {
                    repository.syncNotes()
                }
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authManager.signInWithGoogle(activity)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(user = user, isLoading = false)
                repository.syncNotes()
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.localizedMessage ?: "Error al iniciar sesión con Google"
                )
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        _uiState.value = _uiState.value.copy(user = null, errorMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val authManager: GoogleAuthManager,
        private val repository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authManager, repository) as T
        }
    }
}
