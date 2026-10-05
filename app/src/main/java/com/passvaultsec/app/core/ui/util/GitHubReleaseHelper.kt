package com.passvaultsec.app.core.ui.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utilidad para consultar dinámicamente la última versión/release publicada en GitHub
 * y obtener el enlace directo de descarga del archivo APK.
 */
object GitHubReleaseHelper {

    private const val TAG = "GitHubReleaseHelper"
    private const val GITHUB_REPO_OWNER = "Leoeze83"
    private const val GITHUB_REPO_NAME = "PassVaultSec"
    private const val GITHUB_API_RELEASES = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases"

    const val RELEASES_PAGE_URL = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases"
    const val LATEST_FALLBACK_URL = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"

    @Volatile
    private var cachedDirectDownloadUrl: String = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases"

    @Volatile
    private var cachedReleaseTag: String = "v1.1.1-beta"

    /**
     * Obtiene de forma asíncrona la URL de descarga directa del APK de la última release
     * publicada en el repositorio de GitHub (incluye pre-releases).
     * Si encuentra un asset con extensión .apk, retorna su browser_download_url.
     */
    suspend fun fetchLatestDirectDownloadUrl(): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_RELEASES)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "PassVaultSec-Android")
            }

            if (connection.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = reader.use { it.readText() }
                val releases = JSONArray(jsonString)

                if (releases.length() > 0) {
                    val latestRelease = releases.getJSONObject(0)
                    val tagName = latestRelease.optString("tag_name", "v1.1.1-beta")
                    cachedReleaseTag = tagName

                    val assets = latestRelease.optJSONArray("assets")
                    if (assets != null && assets.length() > 0) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val downloadUrl = asset.optString("browser_download_url")
                            if (downloadUrl.endsWith(".apk", ignoreCase = true)) {
                                Log.d(TAG, "Encontrada descarga directa de APK: $downloadUrl")
                                cachedDirectDownloadUrl = downloadUrl
                                return@withContext downloadUrl
                            }
                        }
                    }

                    val htmlUrl = latestRelease.optString("html_url")
                    if (htmlUrl.isNotBlank()) {
                        cachedDirectDownloadUrl = htmlUrl
                        return@withContext htmlUrl
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando última release en GitHub: ${e.message}")
        } finally {
            connection?.disconnect()
        }

        return@withContext cachedDirectDownloadUrl
    }

    /**
     * Retorna sincrónicamente la última URL directa conocida del APK.
     */
    fun getDirectDownloadUrlSync(): String {
        return cachedDirectDownloadUrl
    }

    /**
     * Retorna el tag actual conocido de la última release.
     */
    fun getLatestReleaseTag(): String {
        return cachedReleaseTag
    }
}
