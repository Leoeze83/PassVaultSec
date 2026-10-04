package com.passvaultsec.app.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.passvaultsec.app.PassVaultApplication
import java.util.concurrent.TimeUnit

/**
 * Worker en segundo plano ejecutado por WorkManager para sincronización
 * offline-first segura con Cloud Firestore cuando hay conectividad de red.
 */
class SyncNotesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? PassVaultApplication
                ?: return Result.failure()

            val result = app.noteRepository.syncNotes()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/**
 * Administrador para programar y coordinar sincronizaciones en segundo plano
 * con WorkManager bajo restricciones de batería y conectividad.
 */
object SyncManager {

    private const val PERIODIC_SYNC_WORK_NAME = "PassVaultSec_Periodic_Sync"
    private const val IMMEDIATE_SYNC_WORK_NAME = "PassVaultSec_Immediate_Sync"

    /**
     * Programa una tarea periódica cada 15 minutos que requiere conexión a Internet.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncNotesWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    /**
     * Dispara una sincronización inmediata cuando el usuario realiza cambios o recupera conectividad.
     */
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<SyncNotesWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }
}
