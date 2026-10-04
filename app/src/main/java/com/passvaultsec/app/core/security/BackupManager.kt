package com.passvaultsec.app.core.security

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.passvaultsec.app.domain.model.Note
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Gestor de copias de seguridad locales cifradas (.pvs) para exportación e importación segura
 * respaldada por cifrado AES-256-GCM y derivación de clave PBKDF2.
 */
object BackupManager {

    private val MAGIC_HEADER = "PVS1".toByteArray(StandardCharsets.US_ASCII) // 4 bytes
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 15000
    private val gson = Gson()

    data class BackupMetadata(
        val version: String = "1.1.0",
        val timestamp: Long = System.currentTimeMillis(),
        val notesCount: Int,
        val notes: List<Note>
    )

    /**
     * Cifra y genera el binario de copia de seguridad (.pvs) a partir de la lista de notas.
     */
    fun createEncryptedBackup(notes: List<Note>, masterPassword: String): ByteArray {
        require(masterPassword.length >= 6) { "La contraseña de respaldo debe tener al menos 6 caracteres" }

        val metadata = BackupMetadata(
            notesCount = notes.size,
            notes = notes
        )
        val jsonBytes = gson.toJson(metadata).toByteArray(StandardCharsets.UTF_8)

        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(masterPassword.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(jsonBytes)

        // Estructura del binario: [MAGIC 4b] + [SALT 16b] + [IV 12b] + [CIPHERTEXT + GCM TAG]
        val output = ByteArray(MAGIC_HEADER.size + SALT_LENGTH + IV_LENGTH + ciphertext.size)
        var offset = 0

        System.arraycopy(MAGIC_HEADER, 0, output, offset, MAGIC_HEADER.size)
        offset += MAGIC_HEADER.size

        System.arraycopy(salt, 0, output, offset, SALT_LENGTH)
        offset += SALT_LENGTH

        System.arraycopy(iv, 0, output, offset, IV_LENGTH)
        offset += IV_LENGTH

        System.arraycopy(ciphertext, 0, output, offset, ciphertext.size)

        return output
    }

    /**
     * Valida, descifra y restaura las notas desde un binario de copia de seguridad (.pvs).
     */
    fun restoreEncryptedBackup(backupBytes: ByteArray, masterPassword: String): Result<List<Note>> {
        return runCatching {
            val minLength = MAGIC_HEADER.size + SALT_LENGTH + IV_LENGTH + 16
            if (backupBytes.size < minLength) {
                throw IllegalArgumentException("Archivo de respaldo inválido o corrupto")
            }

            // 1. Validar cabecera mágica
            for (i in MAGIC_HEADER.indices) {
                if (backupBytes[i] != MAGIC_HEADER[i]) {
                    throw IllegalArgumentException("El archivo no es una copia de seguridad válida de PassVaultSec (.pvs)")
                }
            }

            var offset = MAGIC_HEADER.size

            // 2. Extraer Salt
            val salt = ByteArray(SALT_LENGTH)
            System.arraycopy(backupBytes, offset, salt, 0, SALT_LENGTH)
            offset += SALT_LENGTH

            // 3. Extraer IV
            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(backupBytes, offset, iv, 0, IV_LENGTH)
            offset += IV_LENGTH

            // 4. Extraer Ciphertext
            val ciphertextSize = backupBytes.size - offset
            val ciphertext = ByteArray(ciphertextSize)
            System.arraycopy(backupBytes, offset, ciphertext, 0, ciphertextSize)

            // 5. Derivar clave AES con PBKDF2
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(masterPassword.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            // 6. Descifrar con AES-256-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val decryptedBytes = cipher.doFinal(ciphertext)

            val jsonString = String(decryptedBytes, StandardCharsets.UTF_8)
            val metadata = gson.fromJson(jsonString, BackupMetadata::class.java)
                ?: throw IllegalStateException("Contenido del archivo vacío")

            metadata.notes
        }
    }
}
