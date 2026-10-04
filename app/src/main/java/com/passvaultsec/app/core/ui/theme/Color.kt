package com.passvaultsec.app.core.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFA8C7FA)
val PurpleGrey80 = Color(0xFFC2E7FF)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF0B57D0)
val PurpleGrey40 = Color(0xFF00639B)
val Pink40 = Color(0xFF7D5260)

// Paleta de notas dinámicas (Modo Claro)
val NoteDefault = Color(0xFFFFFFFF)
val NoteRed = Color(0xFFFF8A80)
val NoteOrange = Color(0xFFFFD180)
val NoteYellow = Color(0xFFFFFF8D)
val NoteGreen = Color(0xFFCCFF90)
val NoteTeal = Color(0xFFA7FFEB)
val NoteBlue = Color(0xFF80D8FF)
val NotePurple = Color(0xFFEA80FC)
val NotePink = Color(0xFFFF80AB)
val NoteSand = Color(0xFFF5E1CE)

// Paleta de notas dinámicas (Modo Oscuro)
val NoteDarkDefault = Color(0xFF202124)
val NoteDarkRed = Color(0xFF5C2B29)
val NoteDarkOrange = Color(0xFF614A19)
val NoteDarkYellow = Color(0xFF635D19)
val NoteDarkGreen = Color(0xFF345920)
val NoteDarkTeal = Color(0xFF16504B)
val NoteDarkBlue = Color(0xFF2D555E)
val NoteDarkPurple = Color(0xFF42275E)
val NoteDarkPink = Color(0xFF5B2245)
val NoteDarkSand = Color(0xFF442F19)

// Lista de colores accesibles para el selector
val NotePaletteColors = listOf(
    NoteDefault,
    NoteRed,
    NoteOrange,
    NoteYellow,
    NoteGreen,
    NoteTeal,
    NoteBlue,
    NotePurple,
    NotePink,
    NoteSand
)

// Paleta de colores para selección manual de texto
val NoteTextPaletteColors = listOf(
    0L, // 0L representa "Automático" (calcula contraste por luminancia)
    0xFF000000, // Negro puro
    0xFFFFFFFF, // Blanco puro
    0xFF202124, // Carbón / Gris oscuro
    0xFFF1F3F4, // Gris claro / Nieve
    0xFF1A73E8, // Azul vibrante
    0xFFD93025, // Rojo carmesí
    0xFF1E8E3E, // Verde bosque
    0xFF9334E6, // Púrpura intenso
    0xFFE37400  // Ámbar / Naranja
)

/**
 * Convierte un color de nota guardado (en formato Long ARGB)
 * a su tono correspondiente adaptado a Modo Oscuro o Modo Claro.
 */
fun getAdaptiveNoteColor(rawColor: Long, isDark: Boolean): Color {
    if (!isDark) return Color(rawColor)

    return when (rawColor) {
        NoteDefault.value.toLong() -> NoteDarkDefault
        NoteRed.value.toLong() -> NoteDarkRed
        NoteOrange.value.toLong() -> NoteDarkOrange
        NoteYellow.value.toLong() -> NoteDarkYellow
        NoteGreen.value.toLong() -> NoteDarkGreen
        NoteTeal.value.toLong() -> NoteDarkTeal
        NoteBlue.value.toLong() -> NoteDarkBlue
        NotePurple.value.toLong() -> NoteDarkPurple
        NotePink.value.toLong() -> NoteDarkPink
        NoteSand.value.toLong() -> NoteDarkSand
        0L, 0xFFFFFFFF.toLong() -> NoteDarkDefault
        else -> Color(rawColor)
    }
}

/**
 * Determina el color de texto contrastado según el modo y color de nota.
 * Si el usuario eligió un color de texto manual (customTextColor != null), se respeta.
 * Si no, calcula automáticamente el contraste óptimo por luminancia WCAG.
 */
fun getAdaptiveNoteTextColor(
    rawNoteColor: Long,
    isDark: Boolean,
    customTextColor: Long? = null
): Color {
    if (customTextColor != null && customTextColor != 0L) {
        return Color(customTextColor)
    }

    val noteColor = getAdaptiveNoteColor(rawNoteColor, isDark)
    // Fórmula de luminancia relativa estándar ITU-R BT.709
    val r = noteColor.red
    val g = noteColor.green
    val b = noteColor.blue
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

    return if (luminance > 0.42f) {
        Color(0xFF202124) // Texto oscuro para fondos claros
    } else {
        Color(0xFFF1F3F4) // Texto claro para fondos oscuros
    }
}

/**
 * Determina el color secundario (placeholders, subtítulos) contrastado.
 */
fun getAdaptiveSecondaryTextColor(
    rawNoteColor: Long,
    isDark: Boolean,
    customTextColor: Long? = null
): Color {
    val mainTextColor = getAdaptiveNoteTextColor(rawNoteColor, isDark, customTextColor)
    return mainTextColor.copy(alpha = 0.65f)
}
