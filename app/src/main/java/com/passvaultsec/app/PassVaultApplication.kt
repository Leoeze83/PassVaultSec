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
    }

    companion object {
        lateinit var instance: PassVaultApplication
            private set
    }
}
