package com.passvaultsec.app.presentation

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.passvaultsec.app.PassVaultApplication
import com.passvaultsec.app.core.security.BackupManager
import com.passvaultsec.app.core.ui.theme.PassVaultSecTheme
import com.passvaultsec.app.core.ui.util.NotificationHelper
import com.passvaultsec.app.domain.model.Note
import com.passvaultsec.app.presentation.auth.AccountDialog
import com.passvaultsec.app.presentation.auth.AuthViewModel
import com.passvaultsec.app.presentation.auth.ExportBackupDialog
import com.passvaultsec.app.presentation.auth.ImportBackupDialog
import com.passvaultsec.app.presentation.editor.NoteEditorScreen
import com.passvaultsec.app.presentation.editor.NoteEditorViewModel
import com.passvaultsec.app.presentation.notes.NotesScreen
import com.passvaultsec.app.presentation.notes.NotesViewModel
import android.util.Log
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            PassVaultSecTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    var isAccountDialogOpen by remember { mutableStateOf(false) }
                    var isExportDialogOpen by remember { mutableStateOf(false) }
                    var isImportDialogOpen by remember { mutableStateOf(false) }

                    var pendingExportNotes by remember { mutableStateOf<List<Note>>(emptyList()) }
                    var pendingExportPassword by remember { mutableStateOf<String?>(null) }
                    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }

                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.Factory(authManager, repository)
                    )
                    val authState by authViewModel.uiState.collectAsState()

                    // Launcher para guardar archivo .pvs de exportación
                    val createDocLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
                    ) { uri ->
                        val pwd = pendingExportPassword
                        if (uri != null && pwd != null && pendingExportNotes.isNotEmpty()) {
                            coroutineScope.launch {
                                try {
                                    val backupBytes = BackupManager.createEncryptedBackup(pendingExportNotes, pwd)
                                    contentResolver.openOutputStream(uri)?.use { output ->
                                        output.write(backupBytes)
                                    }
                                    Toast.makeText(context, "Copia de seguridad exportada con éxito", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    pendingExportPassword = null
                                    pendingExportNotes = emptyList()
                                }
                            }
                        }
                    }

                    // Launcher para abrir y leer archivo .pvs de importación
                    val openDocLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        if (uri != null) {
                            coroutineScope.launch {
                                try {
                                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    if (bytes != null && bytes.isNotEmpty()) {
                                        pendingImportBytes = bytes
                                        isImportDialogOpen = true
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al leer archivo: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    // Escucha en tiempo real para recibir notificaciones cuando se invita al usuario
                    val notifiedInvitationIds = remember { mutableSetOf<String>() }
                    LaunchedEffect(authState.user?.email) {
                        val email = authState.user?.email.orEmpty().trim().lowercase()
                        if (email.isNotEmpty() && app.authManager.isUserSignedIn()) {
                            try {
                                app.firestoreService.observeIncomingInvitations(email)
                                    .catch { e ->
                                        Log.w("MainActivity", "Error en flujo de invitaciones: ${e.message}")
                                    }
                                    .collect { invitations ->
                                        for (inv in invitations) {
                                            val id = inv["id"] as? String ?: continue
                                            val noteId = inv["noteId"] as? String ?: continue
                                            val noteTitle = inv["noteTitle"] as? String ?: "Nota Compartida"
                                            val ownerEmail = inv["ownerEmail"] as? String ?: ""
                                            val role = inv["role"] as? String ?: "editor"

                                            if (!notifiedInvitationIds.contains(id)) {
                                                notifiedInvitationIds.add(id)
                                                NotificationHelper.showCollaborationInvitationNotification(
                                                    context = context,
                                                    noteId = noteId,
                                                    noteTitle = noteTitle,
                                                    ownerEmail = ownerEmail,
                                                    role = role
                                                )
                                                repository.syncNotes()
                                            }
                                        }
                                    }
                            } catch (e: Exception) {
                                Log.w("MainActivity", "Excepción colectando invitaciones: ${e.message}")
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "notes"
                    ) {
                        // Pantalla Principal de Notas
                        composable("notes") {
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
                            onExportBackup = {
                                coroutineScope.launch {
                                    val currentNotes = repository.getNotes().first()
                                    if (currentNotes.isEmpty()) {
                                        Toast.makeText(context, "No hay notas para respaldar", Toast.LENGTH_SHORT).show()
                                    } else {
                                        pendingExportNotes = currentNotes
                                        isAccountDialogOpen = false
                                        isExportDialogOpen = true
                                    }
                                }
                            },
                            onImportBackup = {
                                isAccountDialogOpen = false
                                openDocLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                            },
                            onDismiss = {
                                authViewModel.clearError()
                                isAccountDialogOpen = false
                            }
                        )
                    }

                    // Diálogo para definir contraseña de exportación .pvs
                    if (isExportDialogOpen) {
                        ExportBackupDialog(
                            notesCount = pendingExportNotes.size,
                            onConfirm = { pwd ->
                                pendingExportPassword = pwd
                                isExportDialogOpen = false
                                val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                createDocLauncher.launch("PassVaultSec_Backup_$dateStr.pvs")
                            },
                            onDismiss = {
                                isExportDialogOpen = false
                                pendingExportNotes = emptyList()
                            }
                        )
                    }

                    // Diálogo para ingresar contraseña de importación .pvs
                    if (isImportDialogOpen) {
                        ImportBackupDialog(
                            onConfirm = { pwd ->
                                val bytes = pendingImportBytes
                                if (bytes != null) {
                                    val result = BackupManager.restoreEncryptedBackup(bytes, pwd)
                                    result.onSuccess { restoredNotes ->
                                        coroutineScope.launch {
                                            restoredNotes.forEach { note ->
                                                repository.saveNote(note)
                                            }
                                            Toast.makeText(
                                                context,
                                                "Se restauraron ${restoredNotes.size} notas con éxito",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        isImportDialogOpen = false
                                        pendingImportBytes = null
                                    }.onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            "Error al descifrar: ${err.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onDismiss = {
                                isImportDialogOpen = false
                                pendingImportBytes = null
                            }
                        )
                    }
                }
            }
        }
    }
}
