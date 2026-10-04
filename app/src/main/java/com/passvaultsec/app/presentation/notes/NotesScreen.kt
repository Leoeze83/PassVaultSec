package com.passvaultsec.app.presentation.notes

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.passvaultsec.app.core.security.BiometricAuthManager
import com.passvaultsec.app.core.ui.theme.AppThemeMode
import com.passvaultsec.app.core.ui.util.findFragmentActivity
import com.passvaultsec.app.domain.model.Note
import com.passvaultsec.app.presentation.notes.components.NoteCard
import com.passvaultsec.app.presentation.notes.components.SearchAndFilterBar

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    biometricAuthManager: BiometricAuthManager,
    currentUserEmail: String,
    currentUserPhotoUrl: String? = null,
    themeMode: AppThemeMode,
    onToggleTheme: () -> Unit,
    onNavigateToEditor: (noteId: String?, isChecklist: Boolean) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val context = LocalContext.current

    val pinnedNotes = notes.filter { it.isPinned }
    val unpinnedNotes = notes.filter { !it.isPinned }

    val handleNoteClick: (Note) -> Unit = { note ->
        if (note.isLocked) {
            val activity = context.findFragmentActivity()
            if (activity != null && biometricAuthManager.canAuthenticate()) {
                biometricAuthManager.authenticate(
                    activity = activity,
                    title = "Desbloquear Nota Segura",
                    subtitle = "Usa tu huella dactilar o PIN para ver el contenido cifrado",
                    onSuccess = {
                        onNavigateToEditor(note.id, note.isChecklist)
                    },
                    onError = { _, errString ->
                        Toast.makeText(context, "Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(context, "Configura huella o PIN en tu dispositivo", Toast.LENGTH_LONG).show()
            }
        } else {
            onNavigateToEditor(note.id, note.isChecklist)
        }
    }

    Scaffold(
        topBar = {
            SearchAndFilterBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                activeFilter = uiState.activeFilter,
                onFilterChange = viewModel::onFilterChanged,
                isGridLayout = uiState.isGridLayout,
                onToggleLayout = viewModel::toggleLayout,
                userEmail = currentUserEmail,
                userPhotoUrl = currentUserPhotoUrl,
                onProfileClick = onProfileClick,
                themeMode = themeMode,
                onToggleTheme = onToggleTheme
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier.navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón rápido para crear Checklist
                SmallFloatingActionButton(
                    onClick = { onNavigateToEditor(null, true) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.CheckBox, contentDescription = "Nueva lista")
                }

                // Botón FAB principal para crear Nota de texto
                FloatingActionButton(
                    onClick = { onNavigateToEditor(null, false) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva nota")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (notes.isEmpty()) {
                // Estado vacío
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tienes notas aquí",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Toca '+' para crear una nota segura",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else if (uiState.isGridLayout) {
                // Vista en cuadrícula escalonada (2 columnas de tarjetas dinámicas)
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "FIJADAS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                isUnlocked = false,
                                onClick = { handleNoteClick(note) },
                                onTogglePin = { viewModel.togglePin(note) }
                            )
                        }
                    }

                    if (unpinnedNotes.isNotEmpty()) {
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = "OTRAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                        }
                        items(unpinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                isUnlocked = false,
                                onClick = { handleNoteClick(note) },
                                onTogglePin = { viewModel.togglePin(note) }
                            )
                        }
                    }
                }
            } else {
                // Vista en lista lineal
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "FIJADAS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.sp
                            )
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                isUnlocked = false,
                                onClick = { handleNoteClick(note) },
                                onTogglePin = { viewModel.togglePin(note) }
                            )
                        }
                    }

                    if (unpinnedNotes.isNotEmpty()) {
                        if (pinnedNotes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "OTRAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                        items(unpinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                isUnlocked = false,
                                onClick = { handleNoteClick(note) },
                                onTogglePin = { viewModel.togglePin(note) }
                            )
                        }
                    }
                }
            }
        }
    }
}
