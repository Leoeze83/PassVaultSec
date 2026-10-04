package com.passvaultsec.app.domain.model

import java.util.UUID

/**
 * Representa un elemento individual dentro de una nota tipo Lista de Verificación (Checklist).
 */
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)
