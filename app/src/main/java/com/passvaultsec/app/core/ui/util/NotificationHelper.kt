package com.passvaultsec.app.core.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.passvaultsec.app.PassVaultApplication
import com.passvaultsec.app.R
import com.passvaultsec.app.presentation.MainActivity

/**
 * Gestor de notificaciones del sistema Android para invitaciones de colaboración
 * y avisos de seguridad en PassVaultSec.
 */
object NotificationHelper {

    private const val CHANNEL_COLLAB_ID = "collab_invitations_channel"
    private const val CHANNEL_COLLAB_NAME = "Invitaciones a Colaborar"
    private const val CHANNEL_COLLAB_DESC = "Notificaciones cuando eres invitado a colaborar en una nota"

    /**
     * Inicializa los canales de notificación en Android 8.0+ (API 26+).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_COLLAB_ID,
                CHANNEL_COLLAB_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_COLLAB_DESC
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación del sistema cuando el usuario es invitado a colaborar en una nota.
     */
    fun showCollaborationInvitationNotification(
        context: Context,
        noteId: String,
        noteTitle: String,
        ownerEmail: String,
        role: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_NOTE_ID", noteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roleDescription = if (role.equals("editor", ignoreCase = true)) "Editor (Lectura y Escritura)" else "Lector (Solo Lectura)"
        val downloadUrl = "https://github.com/Leoeze83/PassVaultSec/releases"

        val downloadIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val downloadPendingIntent = PendingIntent.getActivity(
            context,
            "download_$noteId".hashCode(),
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COLLAB_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🤝 Invitación a Colaborar")
            .setContentText("$ownerEmail te invitó a '$noteTitle'")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "$ownerEmail te ha añadido como colaborador en la nota '$noteTitle' con permisos de $roleDescription.\n\n" +
                        "📲 Abre la nota en PassVaultSec o descarga/actualiza la aplicación directamente aquí:\n$downloadUrl"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.stat_sys_download, "Descargar App", downloadPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(noteId.hashCode(), notification)

        // Telemetría de notificación recibida con enlace de descarga
        try {
            PassVaultApplication.instance.firestoreService.emitTelemetryAsync(
                eventType = "INVITATION_NOTIFIED",
                category = "COLLAB",
                severity = "INFO",
                detail = "Notificación de colaboración generada con enlace de descarga directa a GitHub Releases"
            )
        } catch (_: Exception) {}
    }
}
