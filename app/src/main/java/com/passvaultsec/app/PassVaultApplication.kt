package com.passvaultsec.app

import android.app.Application
import com.passvaultsec.app.core.auth.GoogleAuthManager
import com.passvaultsec.app.core.security.BiometricAuthManager
import com.passvaultsec.app.core.security.CryptoManager
import com.passvaultsec.app.data.local.AppDatabase
import com.passvaultsec.app.data.remote.FirestoreService
import com.passvaultsec.app.data.repository.NoteRepositoryImpl
import com.passvaultsec.app.domain.repository.NoteRepository

class PassVaultApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var cryptoManager: CryptoManager
        private set

    lateinit var biometricAuthManager: BiometricAuthManager
        private set

    lateinit var authManager: GoogleAuthManager
        private set

    lateinit var firestoreService: FirestoreService
        private set

    lateinit var noteRepository: NoteRepository
        private set

    lateinit var themeManager: com.passvaultsec.app.core.ui.theme.ThemeManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        cryptoManager = CryptoManager()
        biometricAuthManager = BiometricAuthManager(this)
        authManager = GoogleAuthManager(this)
        firestoreService = FirestoreService()
        themeManager = com.passvaultsec.app.core.ui.theme.ThemeManager.getInstance(this)
        noteRepository = NoteRepositoryImpl(
            noteDao = database.noteDao(),
            firestoreService = firestoreService,
            cryptoManager = cryptoManager,
            authManager = authManager
        )

        // Inicializar canales de notificación y tareas en segundo plano
        com.passvaultsec.app.core.ui.util.NotificationHelper.createNotificationChannels(this)
        com.passvaultsec.app.core.sync.SyncManager.schedulePeriodicSync(this)

        // Telemetría de inicio de aplicación e integridad de hardware en tiempo real
        firestoreService.emitTelemetryAsync(
            eventType = "APP_STARTUP_INTEGRITY",
            category = "INTEGRITY",
            severity = "INFO",
            detail = "PassVaultSec iniciado en dispositivo. Hardware Keystore AES-256 verificado."
        )
    }

    companion object {
        lateinit var instance: PassVaultApplication
            private set
    }
}
