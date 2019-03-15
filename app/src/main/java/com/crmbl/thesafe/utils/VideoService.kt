package com.crmbl.thesafe.utils

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback
import android.graphics.Bitmap
import android.app.PendingIntent
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.crmbl.thesafe.R
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import java.io.File
import java.io.IOException


class VideoService: Service() {
    companion object {
        const val PLAYBACK_CHANNEL_ID = "thesafe_playback_channel"
        const val PLAYBACK_NOTIFICATION_ID = 1
        const val SESSION_CHANNEL = "thesafe_media_session"
        const val STOPFOREGROUND_ACTION = "stop_service"
        const val PAUSEFOREGROUND_ACTION = "pause_service"

        private var mediaPath: String? = null
        private var videoServiceListener: VideoServiceListener? = null

        fun setMediaPath(path: String) {
            mediaPath = path
        }
        fun setVideoServiceListener(listener: VideoServiceListener) {
            if (videoServiceListener == null)
                videoServiceListener = listener
        }
    }

    interface VideoServiceListener {
        fun onServiceDestroyed()
    }

    private var player: SimpleExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var mediaSession: MediaSession? = null
    private var mediaSessionConnector: MediaSessionConnector? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val context = this

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
        val file = File(mediaPath)
        val decrypted = CryptoUtil.decrypt(file)
        val byteArrayDataSource = ByteArrayDataSource(decrypted)
        val mediaByteUri = UriByteDataHelper().getUri(decrypted!!)
        val dataSpec = DataSpec(mediaByteUri)
        try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
        val factory = object : DataSource.Factory { override fun createDataSource(): DataSource { return byteArrayDataSource } }
        val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)

        player!!.prepare(mediaSource)
        player!!.playWhenReady = true

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            context, PLAYBACK_CHANNEL_ID, R.string.playback_name, PLAYBACK_NOTIFICATION_ID,
            object : MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player): PendingIntent? { return null }
                override fun getCurrentLargeIcon(player: Player, callback: BitmapCallback): Bitmap? { return null }
                override fun getCurrentContentText(player: Player): String? {
                    return CryptoUtil.decipher(file.extension)
                }
                override fun getCurrentContentTitle(player: Player): String {
                    return file.nameWithoutExtension
                }
            }
        )
        playerNotificationManager!!.setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationStarted(notificationId: Int, notification: Notification) {
                startForeground(notificationId, notification)
            }
            override fun onNotificationCancelled(notificationId: Int) {
                stopSelf()
            }
        })

        playerNotificationManager!!.setColorized(false)
        playerNotificationManager!!.setColor(resources.getColor(R.color.colorAccent, theme))
        playerNotificationManager!!.setUseNavigationActions(false)
        playerNotificationManager!!.setSmallIcon(R.drawable.ic_pepper_icon_small)
        playerNotificationManager!!.setPlayer(player)

        mediaSession = MediaSession(context, SESSION_CHANNEL)
        mediaSession!!.isActive = true

        playerNotificationManager!!.setMediaSessionToken(MediaSessionCompat.Token.fromToken(mediaSession!!.sessionToken))
        mediaSessionConnector = MediaSessionConnector(MediaSessionCompat.fromMediaSession(context, mediaSession))
        mediaSessionConnector!!.mediaSession!!.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
        mediaSessionConnector!!.setPlayer(player, null)
    }

    override fun onDestroy() {
        mediaSession!!.release()
        mediaSessionConnector!!.setPlayer(null, null)
        playerNotificationManager!!.setPlayer(null)
        player!!.release()
        player = null
        mediaPath = null

        videoServiceListener?.onServiceDestroyed()
        videoServiceListener = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when {
            intent.action == STOPFOREGROUND_ACTION -> {
                stopForeground(true)
                stopSelf()
            }
            intent.action == PAUSEFOREGROUND_ACTION -> player!!.playWhenReady = false
        }

        return Service.START_STICKY
    }
}