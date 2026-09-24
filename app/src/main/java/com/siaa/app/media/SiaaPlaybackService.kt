package com.siaa.app.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.siaa.app.MainActivity
import com.siaa.core.runtime.MediaControlEvent

class SiaaPlaybackService : Service() {

    private val binder = LocalBinder()
    private val commandRouter = EarbudCommandRouter()
    private var isPlaying = false

    inner class LocalBinder : Binder() {
        fun getService(): SiaaPlaybackService = this@SiaaPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            when (action) {
                ACTION_PLAY -> updateNotification(title = "SIAA Aprendizaje Auditivo", subtitle = "Reproduciendo...", isPlaying = true)
                ACTION_PAUSE -> updateNotification(title = "SIAA Aprendizaje Auditivo", subtitle = "En pausa", isPlaying = false)
                ACTION_STOP -> stopSelf()
                Intent.ACTION_MEDIA_BUTTON -> {
                    val eventPair = commandRouter.fromMediaButtonIntent(intent)
                    if (eventPair != null) {
                        handleMediaEvent(eventPair.first)
                    }
                }
            }
        }

        startForegroundServiceWithNotification()
        return START_STICKY
    }

    private fun handleMediaEvent(event: MediaControlEvent) {
        when (event) {
            MediaControlEvent.PLAY_PAUSE -> {
                isPlaying = !isPlaying
                updateNotification(title = "SIAA", subtitle = if (isPlaying) "Reproduciendo" else "En pausa", isPlaying = isPlaying)
            }
            MediaControlEvent.NEXT -> {
                // Advance to next exercise
            }
            MediaControlEvent.PREVIOUS -> {
                // Repeat / help
            }
            MediaControlEvent.STOP -> stopSelf()
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification("SIAA — Modo Manos Libres", "Estudiando con audífonos activos", false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun updateNotification(title: String, subtitle: String, isPlaying: Boolean) {
        this.isPlaying = isPlaying
        val notification = buildNotification(title, subtitle, isPlaying)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, subtitle: String, isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, SiaaPlaybackService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pausar" else "Reproducir",
                playPausePendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SIAA Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación de control de audio en segundo plano para SIAA"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        const val CHANNEL_ID = "siaa_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.siaa.app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.siaa.app.ACTION_PAUSE"
        const val ACTION_STOP = "com.siaa.app.ACTION_STOP"
    }
}
