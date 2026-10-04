package com.passvaultsec.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gestor criptográfico de grado militar utilizando AES-256-GCM y Android Keystore.
 * Las llaves privadas nunca salen del hardware seguro (TEE / StrongBox) del dispositivo.
 */
class CryptoManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        private const val KEY_ALIAS = "PassVaultMasterKey"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Cifra un array de bytes usando AES-GCM.
     * Retorna un payload binario con estructura: [IV (12 bytes)] + [Texto Cifrado + GCM Tag]
     */
    fun encrypt(plainBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainBytes)

        val byteBuffer = ByteBuffer.allocate(iv.size + encryptedData.size)
        byteBuffer.put(iv)
        byteBuffer.put(encryptedData)
        return byteBuffer.array()
    }

    /**
     * Descifra un array de bytes generado por el método [encrypt].
     */
    fun decrypt(encryptedPayload: ByteArray): ByteArray {
        require(encryptedPayload.size > IV_LENGTH) { "Payload inválido o dañado" }

        val iv = ByteArray(IV_LENGTH)
        val ciphertext = ByteArray(encryptedPayload.size - IV_LENGTH)

        val buffer = ByteBuffer.wrap(encryptedPayload)
        buffer.get(iv)
        buffer.get(ciphertext)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Cifra una cadena de texto y la devuelve en formato Base64.
     */
    fun encryptString(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val encrypted = encrypt(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * Descifra una cadena en formato Base64 a texto plano.
     */
    fun decryptString(base64Payload: String): String {
        if (base64Payload.isEmpty()) return ""
        val decodedBytes = Base64.decode(base64Payload, Base64.NO_WRAP)
        val decrypted = decrypt(decodedBytes)
        return String(decrypted, Charsets.UTF_8)
    }
}
