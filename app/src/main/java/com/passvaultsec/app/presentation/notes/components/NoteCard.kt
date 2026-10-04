package com.passvaultsec.app.presentation.notes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passvaultsec.app.core.ui.theme.getAdaptiveNoteColor
import com.passvaultsec.app.core.ui.theme.getAdaptiveNoteTextColor
import com.passvaultsec.app.domain.model.Note

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: Note,
    isUnlocked: Boolean = false,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardColor = getAdaptiveNoteColor(note.color, isDark)
    val textColor = getAdaptiveNoteTextColor(note.color, isDark)
    val secondaryTextColor = if (isDark) Color(0xFF9AA0A6) else Color(0xFF5F6368)
    val isLockedAndProtected = note.isLocked && !isUnlocked

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Fila Superior: Título y Botón Fijar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLockedAndProtected) "Nota Protegida" else note.title.ifEmpty { "Sin título" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Fijar nota",
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else secondaryTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido según estado de bloqueo
            if (isLockedAndProtected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Requiere huella o PIN para desbloquear",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor,
                        fontSize = 13.sp
                    )
                }
            } else {
                if (note.isChecklist) {
                    note.checklistItems.take(4).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (item.isDone) secondaryTextColor else textColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                color = if (item.isDone) secondaryTextColor else textColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (note.checklistItems.size > 4) {
                        Text(
                            text = "+ ${note.checklistItems.size - 4} elementos más",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.82f),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Etiquetas y colaboradores
            if (note.labels.isNotEmpty() || note.collaborators.isNotEmpty() || note.isLocked) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (note.isLocked) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Cifrada", fontSize = 11.sp) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isDark) Color(0x33A8C7FA) else Color(0x1F0B57D0)
                            )
                        )
                    }

                    if (note.collaborators.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${note.collaborators.size} colab.",
                                    fontSize = 11.sp
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isDark) Color(0x33C2E7FF) else Color(0x1F00639B)
                            )
                        )
                    }

                    note.labels.forEach { label ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(label, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isDark) Color(0x22FFFFFF) else Color(0x11000000)
                            )
                        )
                    }
                }
            }
        }
    }
}
