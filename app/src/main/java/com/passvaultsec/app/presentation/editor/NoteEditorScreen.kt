package com.passvaultsec.app.presentation.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passvaultsec.app.core.security.BiometricAuthManager
import com.passvaultsec.app.core.ui.theme.getAdaptiveNoteColor
import com.passvaultsec.app.core.ui.theme.getAdaptiveNoteTextColor
import com.passvaultsec.app.core.ui.theme.getAdaptiveSecondaryTextColor
import com.passvaultsec.app.core.ui.util.ImageStorageHelper
import com.passvaultsec.app.core.ui.util.findFragmentActivity
import com.passvaultsec.app.presentation.editor.components.ChecklistSection
import com.passvaultsec.app.presentation.editor.components.ColorSelector
import com.passvaultsec.app.presentation.editor.components.CollaboratorsDialog
import com.passvaultsec.app.presentation.editor.components.EmojiPickerRow
import com.passvaultsec.app.presentation.editor.components.ImageAttachmentsSection
import com.passvaultsec.app.presentation.editor.components.InsertUrlDialog
import com.passvaultsec.app.presentation.editor.components.LinkPreviewCard
import com.passvaultsec.app.presentation.editor.components.TextColorSelector
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val noteBgColor = getAdaptiveNoteColor(note.color, isDark)
    val textColor = getAdaptiveNoteTextColor(note.color, isDark, note.textColor)
    val secondaryTextColor = getAdaptiveSecondaryTextColor(note.color, isDark, note.textColor)
    val iconTint = if (noteBgColor.luminance() > 0.42f) Color(0xFF3C4043) else Color(0xFFE3E3E3)

    // URI temporal para la captura de fotos con la cámara
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher para tomar fotos con la Cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            coroutineScope.launch {
                try {
                    val localPath = ImageStorageHelper.saveImageToInternalStorage(context, tempCameraUri!!)
                    viewModel.onAddImage(localPath)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al guardar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher para seleccionar imágenes y GIFs de la Galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val localPath = ImageStorageHelper.saveImageToInternalStorage(context, uri)
                    viewModel.onAddImage(localPath)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al importar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val locationHelper = remember { com.passvaultsec.app.core.ui.util.LocationHelper(context) }
    val fetchLocation = {
        viewModel.setLocationLoading(true)
        coroutineScope.launch {
            val result = locationHelper.getCurrentNoteLocation()
            result.onSuccess { loc ->
                viewModel.setLocation(loc)
                Toast.makeText(context, "Ubicación agregada: ${loc.placeName}", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                viewModel.setLocationLoading(false)
                Toast.makeText(context, "Error de ubicación: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocation()
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val handleBack = {
        viewModel.saveNote()
        onNavigateBack()
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.saveNote()
        }
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
                            tint = iconTint
                        )
                    }
                },
                actions = {
                    // Fijar nota
                    IconButton(onClick = viewModel::onTogglePin) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Fijar",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else iconTint
                        )
                    }

                    // Candado de seguridad (Huella / PIN)
                    IconButton(onClick = {
                        val activity = context.findFragmentActivity()
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
                            tint = if (note.isLocked) MaterialTheme.colorScheme.error else iconTint
                        )
                    }

                    // Compartir / Colaboradores
                    IconButton(onClick = { viewModel.setCollaboratorDialogOpen(true) }) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Colaboradores",
                            tint = if (note.collaborators.isNotEmpty()) MaterialTheme.colorScheme.primary else iconTint
                        )
                    }

                    // Menú contextual
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = iconTint
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
                                    imageVector = if (note.isChecklist) Icons.AutoMirrored.Filled.ShortText else Icons.Default.CheckBox,
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
        bottomBar = {
            // Barra de herramientas multimedia y estilo (Cámara, Galería/GIF, Color texto, Color fondo, Emojis, URLs)
            Surface(
                color = noteBgColor,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tomar foto con cámara
                    IconButton(
                        onClick = {
                            val uri = ImageStorageHelper.createTempCameraUri(context)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        enabled = uiState.canEdit
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Tomar foto", tint = iconTint)
                    }

                    // Galería (imágenes y GIFs)
                    IconButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = uiState.canEdit
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Insertar imagen o GIF", tint = iconTint)
                    }

                    // Color de texto (Contraste manual o automático)
                    IconButton(
                        onClick = { viewModel.setTextColorPickerOpen(!uiState.isTextColorPickerOpen) },
                        enabled = uiState.canEdit
                    ) {
                        Icon(
                            Icons.Default.FormatColorText,
                            contentDescription = "Color de texto",
                            tint = if (uiState.isTextColorPickerOpen) MaterialTheme.colorScheme.primary else iconTint
                        )
                    }

                    // Color de fondo de la nota
                    IconButton(
                        onClick = { viewModel.setColorPickerOpen(!uiState.isColorPickerOpen) },
                        enabled = uiState.canEdit
                    ) {
                        Icon(
                            Icons.Default.ColorLens,
                            contentDescription = "Color de fondo",
                            tint = if (uiState.isColorPickerOpen) MaterialTheme.colorScheme.primary else iconTint
                        )
                    }

                    // Emojis rápidos
                    IconButton(
                        onClick = { viewModel.setEmojiPickerOpen(!uiState.isEmojiPickerOpen) },
                        enabled = uiState.canEdit
                    ) {
                        Icon(
                            Icons.Default.Mood,
                            contentDescription = "Emoticones",
                            tint = if (uiState.isEmojiPickerOpen) MaterialTheme.colorScheme.primary else iconTint
                        )
                    }

                    // Insertar URL con vista previa
                    IconButton(
                        onClick = { viewModel.setInsertUrlDialogOpen(true) },
                        enabled = uiState.canEdit
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = "Insertar enlace web", tint = iconTint)
                    }

                    // Ubicación geográfica real con Google Maps
                    IconButton(
                        onClick = {
                            if (locationHelper.hasLocationPermission()) {
                                fetchLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = uiState.canEdit && !uiState.isLocationLoading
                    ) {
                        if (uiState.isLocationLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Ubicación GPS",
                                tint = if (note.location != null) Color(0xFFEA4335) else iconTint
                            )
                        }
                    }
                }
            }
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
                        .background(if (isDark) Color(0xFF4A3B18) else Color(0xFFFFF3CD))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Modo Solo Lectura: Fuiste invitado con permisos de lector.",
                        color = if (isDark) Color(0xFFFFD54F) else Color(0xFF856404),
                        fontSize = 13.sp
                    )
                }
            }

            // Imágenes y GIFs adjuntos
            ImageAttachmentsSection(
                imageUris = note.imageUris,
                onRemoveImage = if (uiState.canEdit) { { viewModel.onRemoveImage(it) } } else null
            )

            // Selector horizontal de color de fondo si está abierto
            if (uiState.isColorPickerOpen) {
                ColorSelector(
                    selectedColorLong = note.color,
                    onColorSelected = { selected ->
                        viewModel.onColorChange(selected)
                    }
                )
            }

            // Selector horizontal de color de texto si está abierto
            if (uiState.isTextColorPickerOpen) {
                TextColorSelector(
                    selectedColorLong = note.textColor,
                    onColorSelected = { selectedTextColor ->
                        viewModel.onTextColorChange(selectedTextColor)
                    }
                )
            }

            // Barra rápida de emoticones si está abierta
            if (uiState.isEmojiPickerOpen) {
                EmojiPickerRow(
                    onEmojiSelected = { emoji ->
                        viewModel.onInsertEmoji(emoji)
                    }
                )
            }

            // Indicador de carga de vista previa de enlace
            if (uiState.isLoadingUrlPreview) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Obteniendo vista previa del enlace...",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
            }

            // Vistas previas de enlaces web
            if (note.urlPreviews.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    note.urlPreviews.forEachIndexed { index, preview ->
                        LinkPreviewCard(
                            preview = preview,
                            onRemove = if (uiState.canEdit) { { viewModel.onRemoveUrlPreview(index) } } else null
                        )
                    }
                }
            }

            // Previsualización de ubicación de Google Maps
            if (note.location != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    com.passvaultsec.app.presentation.editor.components.LocationPreviewCard(
                        location = note.location,
                        onRemove = if (uiState.canEdit) { { viewModel.setLocation(null) } } else null
                    )
                }
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
                            color = secondaryTextColor.copy(alpha = 0.7f)
                        )
                    )
                },
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
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
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
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
                                color = secondaryTextColor.copy(alpha = 0.7f)
                            )
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = textColor,
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

    // Diálogo de Insertar Enlace Web
    if (uiState.isInsertUrlDialogOpen) {
        InsertUrlDialog(
            onDismiss = { viewModel.setInsertUrlDialogOpen(false) },
            onConfirm = { url ->
                viewModel.onAddUrlPreview(url)
            }
        )
    }

    // Diálogo de Colaboradores
    if (uiState.isCollaboratorDialogOpen) {
        val isOwner = note.ownerEmail.isEmpty() || note.ownerEmail.equals(currentUserEmail, ignoreCase = true)
        CollaboratorsDialog(
            ownerEmail = note.ownerEmail.ifEmpty { currentUserEmail },
            collaborators = note.collaborators,
            isOwner = isOwner,
            noteTitle = note.title,
            onAddCollaborator = { email, role ->
                viewModel.addCollaborator(email, role)
            },
            onRemoveCollaborator = { email ->
                viewModel.removeCollaborator(email)
            },
            onInviteViaApp = { email, role ->
                val roleDesc = if (role == com.passvaultsec.app.domain.model.CollaboratorRole.EDITOR) "Editor (Lectura y Escritura)" else "Lector (Solo Lectura)"
                val downloadUrl = com.passvaultsec.app.core.ui.util.GitHubReleaseHelper.getDirectDownloadUrlSync()
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(email))
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Invitación para colaborar en PassVaultSec")
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        "👋 ¡Hola!\n\n" +
                        "Te he invitado a colaborar en la nota '${note.title.ifBlank { "Sin título" }}' en PassVaultSec con permisos de $roleDesc.\n\n" +
                        "📲 Si aún no tienes instalada la app o necesitas la última versión, descárgala directamente aquí:\n" +
                        "$downloadUrl\n\n" +
                        "¡Abre PassVaultSec con tu correo ($email) para sincronizarla automáticamente!"
                    )
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "Enviar invitación a $email"))
            },
            onShareEncryptedLink = {
                val (secureLink, passphrase) = com.passvaultsec.app.core.security.EncryptedShareManager.createEncryptedSharePackage(note)
                com.passvaultsec.app.core.security.EncryptedShareManager.launchShareIntent(
                    context = context,
                    noteTitle = note.title.ifBlank { "Sin título" },
                    secureLink = secureLink,
                    passphrase = passphrase,
                    hoursValid = 24
                )
            },
            onDismiss = { viewModel.setCollaboratorDialogOpen(false) }
        )
    }
}
