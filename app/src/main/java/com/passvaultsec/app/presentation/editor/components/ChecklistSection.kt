package com.passvaultsec.app.presentation.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passvaultsec.app.domain.model.ChecklistItem

@Composable
fun ChecklistSection(
    items: List<ChecklistItem>,
    onItemChange: (index: Int, updatedItem: ChecklistItem) -> Unit,
    onItemDelete: (index: Int) -> Unit,
    onAddItem: () -> Unit,
    canEdit: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isDone,
                    onCheckedChange = { isChecked ->
                        if (canEdit) {
                            onItemChange(index, item.copy(isDone = isChecked))
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = secondaryTextColor,
                        uncheckedColor = textColor.copy(alpha = 0.6f),
                        checkmarkColor = MaterialTheme.colorScheme.surface
                    )
                )

                TextField(
                    value = item.text,
                    onValueChange = { newText ->
                        if (canEdit) {
                            onItemChange(index, item.copy(text = newText))
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Elemento de lista",
                            color = secondaryTextColor.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = TextStyle(
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                        color = if (item.isDone) secondaryTextColor else textColor,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    enabled = canEdit
                )

                if (canEdit) {
                    IconButton(
                        onClick = { onItemDelete(index) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Eliminar elemento",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (canEdit) {
            // Fila para agregar nuevo elemento a la lista
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddItem() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir elemento",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Elemento de lista",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )
            }
        }
    }
}
