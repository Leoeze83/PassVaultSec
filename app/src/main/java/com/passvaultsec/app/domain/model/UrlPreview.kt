package com.passvaultsec.app.domain.model

/**
 * Modelo de datos para previsualización enriquecida de enlaces web (OpenGraph).
 */
data class UrlPreview(
    val url: String,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val domain: String = ""
)
