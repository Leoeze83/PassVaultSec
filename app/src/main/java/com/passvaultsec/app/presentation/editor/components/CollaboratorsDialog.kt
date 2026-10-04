package com.passvaultsec.app.presentation.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passvaultsec.app.domain.model.Collaborator
import com.passvaultsec.app.domain.model.CollaboratorRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaboratorsDialog(
    ownerEmail: String,
    collaborators: Map<String, Collaborator>,
    isOwner: Boolean,
    onAddCollaborator: (email: String, role: CollaboratorRole) -> Unit,
    onRemoveCollaborator: (email: String) -> Unit,
    onDismiss: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(CollaboratorRole.EDITOR) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir Nota")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Dueño de la nota
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = ownerEmail.ifEmpty { "Tú (Propietario)" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Propietario",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lista de colaboradores existentes
                if (collaborators.isNotEmpty()) {
                    Text(
                        text = "Colaboradores con acceso:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        items(collaborators.values.toList()) { collab ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = collab.email,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (collab.role == CollaboratorRole.EDITOR) "Puede editar" else "Solo ver",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }

                                if (isOwner) {
                                    IconButton(
                                        onClick = { onRemoveCollaborator(collab.email) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Quitar colaborador",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Sección para añadir nuevo colaborador (solo para el dueño)
                if (isOwner) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Añadir cuenta de Google:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Correo Gmail") },
                        placeholder = { Text("ejemplo@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Selector de Rol (Editor / Lector)
                    ExposedDropdownMenuBox(
                        expanded = roleDropdownExpanded,
                        onExpandedChange = { roleDropdownExpanded = !roleDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (selectedRole == CollaboratorRole.EDITOR) "Editor (Lectura y Escritura)" else "Lector (Solo Lectura)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            label = { Text("Permiso") }
                        )

                        ExposedDropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editor (Lectura y Escritura)") },
                                onClick = {
                                    selectedRole = CollaboratorRole.EDITOR
                                    roleDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Lector (Solo Lectura)") },
                                onClick = {
                                    selectedRole = CollaboratorRole.VIEWER
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (emailInput.isNotBlank() && emailInput.contains("@")) {
                                onAddCollaborator(emailInput.trim(), selectedRole)
                                emailInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = emailInput.isNotBlank() && emailInput.contains("@")
                    ) {
                        Text("Añadir colaborador")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo")
            }
        }
    )
}
