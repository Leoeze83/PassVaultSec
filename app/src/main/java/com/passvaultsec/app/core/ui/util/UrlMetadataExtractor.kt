package com.passvaultsec.app.core.ui.util

import android.net.Uri
import android.util.Log
import com.passvaultsec.app.domain.model.UrlPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object UrlMetadataExtractor {

    private const val TAG = "UrlMetadataExtractor"
    private const val TIMEOUT_MS = 3500

    private val URL_REGEX = Pattern.compile(
        "\\b(https?://[a-zA-Z0-9+&@#/%?=~_|!:,.;]*[a-zA-Z0-9+&@#/%=~_|])",
        Pattern.CASE_INSENSITIVE
    )

    fun extractUrls(text: String): List<String> {
        val matcher = URL_REGEX.matcher(text)
        val urls = mutableListOf<String>()
        while (matcher.find()) {
            val url = matcher.group()
            if (!urls.contains(url)) {
                urls.add(url)
            }
        }
        return urls
    }

    suspend fun fetchUrlPreview(urlString: String): UrlPreview? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            val host = url.host ?: ""

            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                instanceFollowRedirects = true
            }

            if (connection.responseCode !in 200..299) {
                return@withContext UrlPreview(url = urlString, domain = host)
            }

            val contentType = connection.contentType ?: ""
            if (!contentType.contains("text/html", ignoreCase = true)) {
                return@withContext UrlPreview(url = urlString, domain = host)
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val htmlBuilder = StringBuilder()
            var linesRead = 0
            var line: String? = reader.readLine()
            // Leemos solo el bloque inicial (<head>) para máxima velocidad
            while (line != null && linesRead < 150) {
                htmlBuilder.append(line).append("\n")
                if (line.contains("</head>", ignoreCase = true)) break
                line = reader.readLine()
                linesRead++
            }
            reader.close()

            val html = htmlBuilder.toString()

            val ogTitle = extractMetaTag(html, "og:title")
                ?: extractTagContent(html, "<title>(.*?)</title>")
                ?: host
            val ogDesc = extractMetaTag(html, "og:description")
                ?: extractMetaName(html, "description")
                ?: ""
            val ogImage = extractMetaTag(html, "og:image") ?: ""

            UrlPreview(
                url = urlString,
                title = ogTitle.trim(),
                description = ogDesc.trim(),
                imageUrl = ogImage.trim(),
                domain = host
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo extraer preview para $urlString: ${e.message}")
            val domain = try { Uri.parse(urlString).host.orEmpty() } catch (_: Exception) { "" }
            UrlPreview(url = urlString, domain = domain)
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractMetaTag(html: String, property: String): String? {
        val pattern = Pattern.compile(
            """<meta\s+[^>]*property=["']$property["'][^>]*content=["']([^"']*)["']""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)

        val reversedPattern = Pattern.compile(
            """<meta\s+[^>]*content=["']([^"']*)["'][^>]*property=["']$property["']""",
            Pattern.CASE_INSENSITIVE
        )
        val reversedMatcher = reversedPattern.matcher(html)
        if (reversedMatcher.find()) return reversedMatcher.group(1)

        return null
    }

    private fun extractMetaName(html: String, name: String): String? {
        val pattern = Pattern.compile(
            """<meta\s+[^>]*name=["']$name["'][^>]*content=["']([^"']*)["']""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun extractTagContent(html: String, regex: String): String? {
        val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }
}
