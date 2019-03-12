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
import android.graphics.BitmapFactory
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.content.ContextCompat
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
        const val VIDEO_PATH = "video_path"
        const val PLAYBACK_CHANNEL_ID = "thesafe_playback_channel"
        const val PLAYBACK_NOTIFICATION_ID = 1
        const val SESSION_CHANNEL = "thesafe_media_session"
        const val STOPFOREGROUND_ACTION = "stop_service"
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

        val theSafeFolder = ContextCompat.getExternalFilesDirs(applicationContext, null)
            .last{ f -> f.name == "files" && f.isDirectory }
            .listFiles().first{ f -> f.name == "Download" && f.isDirectory }
            .listFiles().first{ f -> f.name == ".blob" && f.isDirectory && f.isHidden }
        var file : File? = null
        for (_file in theSafeFolder.listFiles()) {
            if (CryptoUtil.decipher(_file.nameWithoutExtension) == "big_buck_bunny") {file = _file; break}
        }

        val decrypted = CryptoUtil.decrypt(java.io.File(file!!.path))
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
                override fun getCurrentContentTitle(player: Player): String {
                    return file.nameWithoutExtension
                }
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    return null
                }
                override fun getCurrentContentText(player: Player): String? {
                    return "Description of the file"
                }
                override fun getCurrentLargeIcon(player: Player, callback: BitmapCallback): Bitmap? {
                    return BitmapFactory.decodeResource(resources, R.drawable.ic_headset_white_24dp)
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
        playerNotificationManager!!.setColorized(true)
        //TODO find better color
        playerNotificationManager!!.setColor(resources.getColor(R.color.colorAccent, theme))
        playerNotificationManager!!.setUseNavigationActions(false)
        playerNotificationManager!!.setUseChronometer(true)
        playerNotificationManager!!.setUsePlayPauseActions(true)
        playerNotificationManager!!.setPlayer(player)

        mediaSession = MediaSession(context, SESSION_CHANNEL)
        mediaSession!!.isActive = true
        playerNotificationManager!!.setMediaSessionToken(MediaSessionCompat.Token.fromToken(mediaSession!!.sessionToken))
        mediaSessionConnector = MediaSessionConnector(MediaSessionCompat.fromMediaSession(context, mediaSession))
        mediaSessionConnector!!.setPlayer(player, null)
    }

    override fun onDestroy() {
        mediaSession!!.release()
        mediaSessionConnector!!.setPlayer(null, null)
        playerNotificationManager!!.setPlayer(null)
        player!!.release()
        player = null

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == STOPFOREGROUND_ACTION) {
            stopForeground(true)
            stopSelf()
        }

        return Service.START_STICKY
    }
}