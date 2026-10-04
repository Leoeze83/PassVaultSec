package com.passvaultsec.app.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Gestor de autenticación biométrica y credenciales del dispositivo (PIN / Huella / Patrón).
 * Compatible con Android 13 y versiones superiores.
 */
class BiometricAuthManager(private val context: Context) {

    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * Determina si el dispositivo cuenta con sensores biométricos o credenciales de bloqueo configuradas.
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Muestra el diálogo de autenticación nativo de Android (BiometricPrompt).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Autenticación Requerida",
        subtitle: String = "Usa tu huella dactilar o PIN para continuar",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    com.passvaultsec.app.PassVaultApplication.instance.firestoreService.emitTelemetryAsync(
                        eventType = "BIOMETRIC_AUTH_SUCCESS",
                        category = "CRYPTO",
                        severity = "INFO",
                        detail = "Autenticación biométrica validada exitosamente en hardware"
                    )
                } catch (_: Exception) {}
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                try {
                    val isLockout = errorCode == BiometricPrompt.ERROR_LOCKOUT || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
                    com.passvaultsec.app.PassVaultApplication.instance.firestoreService.emitTelemetryAsync(
                        eventType = if (isLockout) "BIOMETRIC_LOCKOUT" else "BIOMETRIC_ERROR",
                        category = "ALERT",
                        severity = if (isLockout) "CRITICAL" else "WARNING",
                        detail = "Evento biométrico ($errorCode): $errString"
                    )
                } catch (_: Exception) {}
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                try {
                    com.passvaultsec.app.PassVaultApplication.instance.firestoreService.emitTelemetryAsync(
                        eventType = "BIOMETRIC_AUTH_FAILED",
                        category = "ALERT",
                        severity = "WARNING",
                        detail = "Fallo de autenticación biométrica: sensor rechazó la huella o rostro"
                    )
                } catch (_: Exception) {}
                onFailed()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        val prompt = BiometricPrompt(activity, executor, callback)
        prompt.authenticate(promptInfo)
    }
}
