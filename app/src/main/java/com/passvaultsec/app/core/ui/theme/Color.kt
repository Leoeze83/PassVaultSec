package com.passvaultsec.app.core.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFA8C7FA)
val PurpleGrey80 = Color(0xFFC2E7FF)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF0B57D0)
val PurpleGrey40 = Color(0xFF00639B)
val Pink40 = Color(0xFF7D5260)

// Paleta de notas estilo Google Keep (Modo Claro)
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

// Paleta de notas estilo Google Keep (Modo Oscuro)
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
val KeepNoteColors = listOf(
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
 */
fun getAdaptiveNoteTextColor(rawColor: Long, isDark: Boolean): Color {
    return if (isDark) {
        Color(0xFFE8EAED)
    } else {
        Color(0xFF202124)
    }
}
