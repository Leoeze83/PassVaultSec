package com.passvaultsec.app.presentation.notes.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.passvaultsec.app.presentation.notes.NotesFilter

@Composable
fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilter: NotesFilter,
    onFilterChange: (NotesFilter) -> Unit,
    isGridLayout: Boolean,
    onToggleLayout: () -> Unit,
    userEmail: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Barra de búsqueda flotante estilo Google Keep
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = CircleShape,
            color = Color(0xFFF1F3F4),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )

                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Buscar en tus notas…", color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Borrar búsqueda",
                            tint = Color.Gray
                        )
                    }
                }

                // Cambiar entre vista en Cuadrícula y Lista
                IconButton(onClick = onToggleLayout) {
                    Icon(
                        imageVector = if (isGridLayout) Icons.Default.ViewAgenda else Icons.Default.GridView,
                        contentDescription = "Alternar diseño",
                        tint = Color.DarkGray
                    )
                }

                // Avatar / Perfil Google
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Cuenta Google",
                        tint = if (userEmail.isNotEmpty()) Color(0xFF1A73E8) else Color.Gray,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // Fila horizontal de Chips de Filtro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeFilter == NotesFilter.ALL,
                onClick = { onFilterChange(NotesFilter.ALL) },
                label = { Text("Todas") },
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = activeFilter == NotesFilter.PINNED,
                onClick = { onFilterChange(NotesFilter.PINNED) },
                label = { Text("Fijadas") },
                leadingIcon = {
                    Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = activeFilter == NotesFilter.LOCKED,
                onClick = { onFilterChange(NotesFilter.LOCKED) },
                label = { Text("Protegidas") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = activeFilter == NotesFilter.ARCHIVED,
                onClick = { onFilterChange(NotesFilter.ARCHIVED) },
                label = { Text("Archivadas") },
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}
