package com.passvaultsec.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AesGcmCipherTest {

    @Test
    fun `test AES-256 GCM encryption and decryption round-trip`() {
        val originalText = "Nota confidencial con contraseñas y claves bancarias #12345"

        // Generar clave AES de 256 bits para prueba unitaria
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey: SecretKey = keyGen.generateKey()

        // 1. Cifrado
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val encryptedBytes = cipher.doFinal(originalText.toByteArray(Charsets.UTF_8))

        // Empaquetar IV + Datos cifrados
        val payload = ByteBuffer.allocate(iv.size + encryptedBytes.size)
            .put(iv)
            .put(encryptedBytes)
            .array()

        assertNotEquals(originalText, String(encryptedBytes, Charsets.UTF_8))

        // 2. Descifrado
        val extractedIv = ByteArray(12)
        val extractedCiphertext = ByteArray(payload.size - 12)
        val buffer = ByteBuffer.wrap(payload)
        buffer.get(extractedIv)
        buffer.get(extractedCiphertext)

        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val decryptSpec = GCMParameterSpec(128, extractedIv)
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, decryptSpec)

        val decryptedBytes = decryptCipher.doFinal(extractedCiphertext)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)

        // Verificación
        assertEquals(originalText, decryptedText)
    }
}
