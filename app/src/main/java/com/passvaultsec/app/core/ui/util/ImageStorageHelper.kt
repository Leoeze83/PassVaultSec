package com.passvaultsec.app.core.ui.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageStorageHelper {

    fun createTempCameraUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, "camera_images").apply { mkdirs() }
        val tempFile = File(cacheDir, "cam_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "note_images").apply { mkdirs() }
        val mimeType = context.contentResolver.getType(sourceUri) ?: ""
        val extension = when {
            mimeType.contains("gif", ignoreCase = true) -> "gif"
            mimeType.contains("png", ignoreCase = true) -> "png"
            mimeType.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }

        val targetFile = File(imagesDir, "img_${UUID.randomUUID()}.$extension")
        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        val outputStream = FileOutputStream(targetFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        targetFile.absolutePath
    }
}
