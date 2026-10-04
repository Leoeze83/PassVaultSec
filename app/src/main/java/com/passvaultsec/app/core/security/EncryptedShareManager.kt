package com.passvaultsec.app.core.security

import android.content.Context
import android.content.Intent
import android.util.Base64
import com.google.gson.Gson
import com.passvaultsec.app.domain.model.ChecklistItem
import com.passvaultsec.app.domain.model.Note
import com.passvaultsec.app.domain.model.NoteLocation
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Gestor para compartir notas mediante enlaces y paquetes cifrados temporales con caducidad.
 */
object EncryptedShareManager {

    private val gson = Gson()
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val ITERATIONS = 5000

    data class ShareableData(
        val title: String,
        val content: String,
        val isChecklist: Boolean,
        val checklistItems: List<ChecklistItem>,
        val location: NoteLocation?,
        val createdAt: Long,
        val expiresAt: Long
    )

    /**
     * Genera un paquete cifrado con expiración y contraseña aleatoria.
     * Retorna el texto del enlace y la clave de acceso para el destinatario.
     */
    fun createEncryptedSharePackage(
        note: Note,
        expirationMillis: Long = 24 * 60 * 60 * 1000L // 24 horas por defecto
    ): Pair<String, String> {
        val randomPassphrase = generateRandomPassphrase(8)
        val now = System.currentTimeMillis()
        val expiresAt = now + expirationMillis

        val data = ShareableData(
            title = note.title,
            content = note.content,
            isChecklist = note.isChecklist,
            checklistItems = note.checklistItems,
            location = note.location,
            createdAt = now,
            expiresAt = expiresAt
        )

        val jsonString = gson.toJson(data)
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(randomPassphrase.toCharArray(), salt, ITERATIONS, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(jsonString.toByteArray(StandardCharsets.UTF_8))

        // Empaquetado: [Salt: 16b] + [IV: 12b] + [Ciphertext]
        val combined = ByteArray(salt.size + iv.size + ciphertext.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(ciphertext, 0, combined, salt.size + iv.size, ciphertext.size)

        val base64Payload = Base64.encodeToString(combined, Base64.URL_SAFE or Base64.NO_WRAP)
        val secureLink = "https://passvaultsec.app/share?d=$base64Payload"

        return Pair(secureLink, randomPassphrase)
    }

    /**
     * Lanza el diálogo nativo de compartir de Android con el enlace cifrado y la clave.
     */
    fun launchShareIntent(
        context: Context,
        noteTitle: String,
        secureLink: String,
        passphrase: String,
        hoursValid: Int = 24
    ) {
        val shareMessage = buildString {
            append("🔐 Nota Segura Cifrada (PassVaultSec)\n")
            append("Título: \"$noteTitle\"\n")
            append("⏳ Válida por $hoursValid horas.\n\n")
            append("🔗 Enlace Cifrado: $secureLink\n\n")
            append("🔑 Clave de Acceso para Descifrar: $passphrase\n\n")
            append("Copia este enlace o ábrelo en PassVaultSec para ver la nota.")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            putExtra(Intent.EXTRA_TITLE, "Nota Cifrada: $noteTitle")
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Compartir nota cifrada")
        context.startActivity(shareIntent)
    }

    private fun generateRandomPassphrase(length: Int): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}
