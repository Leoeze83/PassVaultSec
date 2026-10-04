package com.passvaultsec.app.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.passvaultsec.app.PassVaultApplication
import com.passvaultsec.app.core.ui.theme.PassVaultSecTheme
import com.passvaultsec.app.presentation.auth.AccountDialog
import com.passvaultsec.app.presentation.auth.AuthViewModel
import com.passvaultsec.app.presentation.editor.NoteEditorScreen
import com.passvaultsec.app.presentation.editor.NoteEditorViewModel
import com.passvaultsec.app.presentation.notes.NotesScreen
import com.passvaultsec.app.presentation.notes.NotesViewModel

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PassVaultApplication
        val repository = app.noteRepository
        val biometricAuthManager = app.biometricAuthManager
        val authManager = app.authManager

        setContent {
            val themeMode by app.themeManager.themeMode.collectAsState()

            PassVaultSecTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    var isAccountDialogOpen by remember { mutableStateOf(false) }

                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.Factory(authManager, repository)
                    )
                    val authState by authViewModel.uiState.collectAsState()

                    NavHost(
                        navController = navController,
                        startDestination = "notes"
                    ) {
                        // Pantalla Principal de Notas
                        composable("notes") {
                            // En la pantalla general permitimos visualización normal
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

                            val notesViewModel: NotesViewModel = viewModel(
                                factory = NotesViewModel.Factory(repository)
                            )

                            NotesScreen(
                                viewModel = notesViewModel,
                                biometricAuthManager = biometricAuthManager,
                                currentUserEmail = authState.user?.email.orEmpty(),
                                themeMode = themeMode,
                                onToggleTheme = { app.themeManager.toggleTheme() },
                                onNavigateToEditor = { noteId, isChecklist ->
                                    val destination = if (noteId != null) {
                                        "editor/$noteId?isChecklist=$isChecklist"
                                    } else {
                                        "editor/new?isChecklist=$isChecklist"
                                    }
                                    navController.navigate(destination)
                                },
                                onProfileClick = {
                                    authViewModel.clearError()
                                    isAccountDialogOpen = true
                                }
                            )
                        }

                        // Pantalla de Edición / Creación de Nota
                        composable(
                            route = "editor/{noteId}?isChecklist={isChecklist}",
                            arguments = listOf(
                                navArgument("noteId") { type = NavType.StringType },
                                navArgument("isChecklist") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            val rawNoteId = backStackEntry.arguments?.getString("noteId")
                            val isChecklist = backStackEntry.arguments?.getBoolean("isChecklist") ?: false
                            val noteId = if (rawNoteId == "new") null else rawNoteId

                            val editorViewModel: NoteEditorViewModel = viewModel(
                                key = "editor_${rawNoteId ?: "new"}",
                                factory = NoteEditorViewModel.Factory(
                                    noteId = noteId,
                                    startAsChecklist = isChecklist,
                                    repository = repository,
                                    authManager = authManager
                                )
                            )

                            // Activamos FLAG_SECURE para evitar capturas si la nota es confidencial
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE
                            )

                            NoteEditorScreen(
                                viewModel = editorViewModel,
                                biometricAuthManager = biometricAuthManager,
                                currentUserEmail = authState.user?.email.orEmpty(),
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    // Diálogo de Cuenta Google y Configuración
                    if (isAccountDialogOpen) {
                        AccountDialog(
                            user = authState.user,
                            isLoading = authState.isLoading,
                            errorMessage = authState.errorMessage,
                            currentThemeMode = themeMode,
                            onThemeModeSelected = { selectedMode ->
                                app.themeManager.setThemeMode(selectedMode)
                            },
                            onSignIn = {
                                authViewModel.signInWithGoogle(this@MainActivity)
                            },
                            onSignOut = {
                                authViewModel.signOut()
                            },
                            onDismiss = {
                                authViewModel.clearError()
                                isAccountDialogOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}
