package com.passvaultsec.app.presentation.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.passvaultsec.app.core.security.BiometricAuthManager
import com.passvaultsec.app.presentation.editor.components.ChecklistSection
import com.passvaultsec.app.presentation.editor.components.ColorSelector
import com.passvaultsec.app.presentation.editor.components.CollaboratorsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    biometricAuthManager: BiometricAuthManager,
    currentUserEmail: String,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val note = uiState.note
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    val noteBgColor = Color(note.color)

    val handleBack = {
        viewModel.saveNote()
        onNavigateBack()
    }

    BackHandler {
        handleBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.Black.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    // Fijar nota
                    IconButton(onClick = viewModel::onTogglePin) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Fijar",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else Color.DarkGray
                        )
                    }

                    // Candado de seguridad (Huella / PIN)
                    IconButton(onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            viewModel.onToggleLock(
                                activity = activity,
                                biometricAuthManager = biometricAuthManager,
                                onSuccess = {
                                    val msg = if (note.isLocked) "Nota desprotegida" else "Nota protegida con huella y cifrado"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }) {
                        Icon(
                            imageVector = if (note.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Proteger",
                            tint = if (note.isLocked) MaterialTheme.colorScheme.error else Color.DarkGray
                        )
                    }

                    // Selector de color
                    IconButton(onClick = { viewModel.setColorPickerOpen(!uiState.isColorPickerOpen) }) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = "Color de nota",
                            tint = Color.DarkGray
                        )
                    }

                    // Compartir / Colaboradores
                    IconButton(onClick = { viewModel.setCollaboratorDialogOpen(true) }) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Colaboradores",
                            tint = if (note.collaborators.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.DarkGray
                        )
                    }

                    // Menú contextual
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = Color.DarkGray
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (note.isChecklist) "Texto estándar" else "Casillas de verificación") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (note.isChecklist) Icons.Default.ShortText else Icons.Default.CheckBox,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                viewModel.onToggleChecklist()
                                menuExpanded = false
                            }
                        )

                        if (!uiState.isNewNote) {
                            DropdownMenuItem(
                                text = { Text("Eliminar nota", color = Color.Red) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.deleteNote { onNavigateBack() }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = noteBgColor)
            )
        },
        containerColor = noteBgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Barra de solo lectura si no es editor
            if (!uiState.canEdit) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3CD))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Modo Solo Lectura: Fuiste invitado con permisos de lector.",
                        color = Color(0xFF856404),
                        fontSize = 13.sp
                    )
                }
            }

            // Selector horizontal de colores si está abierto
            if (uiState.isColorPickerOpen) {
                ColorSelector(
                    selectedColorLong = note.color,
                    onColorSelected = { selected ->
                        viewModel.onColorChange(selected)
                    }
                )
            }

            // Título de la nota
            TextField(
                value = note.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = {
                    Text(
                        "Título",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    )
                },
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.87f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                enabled = uiState.canEdit
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cuerpo: Checklist o Texto Continuo
            if (note.isChecklist) {
                ChecklistSection(
                    items = note.checklistItems,
                    onItemChange = viewModel::onChecklistItemChange,
                    onItemDelete = viewModel::onChecklistItemDelete,
                    onAddItem = viewModel::onAddChecklistItem,
                    canEdit = uiState.canEdit,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                TextField(
                    value = note.content,
                    onValueChange = viewModel::onContentChange,
                    placeholder = {
                        Text(
                            "Nota",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black.copy(alpha = 0.87f),
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    enabled = uiState.canEdit
                )
            }
        }
    }

    // Diálogo de Colaboradores
    if (uiState.isCollaboratorDialogOpen) {
        val isOwner = note.ownerEmail.isEmpty() || note.ownerEmail.equals(currentUserEmail, ignoreCase = true)
        CollaboratorsDialog(
            ownerEmail = note.ownerEmail.ifEmpty { currentUserEmail },
            collaborators = note.collaborators,
            isOwner = isOwner,
            onAddCollaborator = { email, role ->
                viewModel.addCollaborator(email, role)
            },
            onRemoveCollaborator = { email ->
                viewModel.removeCollaborator(email)
            },
            onDismiss = { viewModel.setCollaboratorDialogOpen(false) }
        )
    }
}
