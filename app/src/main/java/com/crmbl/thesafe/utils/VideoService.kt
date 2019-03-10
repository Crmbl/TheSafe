package com.crmbl.thesafe.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.crmbl.thesafe.R
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import kotlinx.coroutines.*
import java.io.IOException

//TODO release on destroy
// disable swipe ?
// add checks ?
class VideoService: Service() {
    companion object {
        const val VIDEO_PATH = "videoPath"
        const val PLAY_PAUSE_ACTION = "playPauseAction"
        const val STOP_ACTION = "stopAction"
        const val REPEAT_ACTION = "repeatAction"
        const val CLOSE_ACTION = "closeAction"
        const val NOTIFICAITON_ID = 0
    }

    private lateinit var exoPlayer: SimpleExoPlayer

    override fun onBind(intent: Intent?): IBinder {
        intent?.let {
            exoPlayer.playWhenReady = true
            loadMedia(intent.getStringExtra(VIDEO_PATH))
            displayNotification()
        }
        return VideoServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val stopAction = it.getIntExtra(STOP_ACTION, -1)
            when (stopAction) {
                0 -> {
                    exoPlayer.playWhenReady = false
                    exoPlayer.stop()
                    displayNotification()
                }
            }

            val pauseAction = it.getIntExtra(PLAY_PAUSE_ACTION, -1)
            when (pauseAction) {
                0 -> {
                    exoPlayer.playWhenReady = false
                    displayNotification()
                }
                1 -> {
                    exoPlayer.playWhenReady = true
                    displayNotification()
                }
            }

            val repeatAction = it.getIntExtra(REPEAT_ACTION, -1)
            when (repeatAction) {
                0 -> {
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                    displayNotification()
                }
                1 -> {
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                    displayNotification()
                }
            }

            val closeAction = it.getIntExtra(CLOSE_ACTION, -1)
            when (closeAction) {
                1 -> {
                    exoPlayer.stop()
                    exoPlayer.release()
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                        .cancel(NOTIFICAITON_ID)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    inner class VideoServiceBinder : Binder() {
        fun getExoPlayerInstance() = exoPlayer
    }

    private fun loadMedia(fileName: String) {
        var decrypted: ByteArray? = null
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val deferred = async(Dispatchers.Default) {
                decrypted = CryptoUtil.decrypt(java.io.File(fileName))
            }

            deferred.await()
            val byteArrayDataSource = ByteArrayDataSource(decrypted)
            val mediaByteUri = UriByteDataHelper().getUri(decrypted!!)
            val dataSpec = DataSpec(mediaByteUri)
            try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
            val factory = object : DataSource.Factory { override fun createDataSource(): DataSource { return byteArrayDataSource } }
            val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)
            exoPlayer.prepare(mediaSource)
        }
    }

    private fun displayNotification() {
        val remoteView = RemoteViews(packageName, R.layout.video_notification)

        if (exoPlayer.playWhenReady)
            remoteView.setImageViewResource(R.id.play_pause_player_btn, R.drawable.ic_pause_white_24dp)
        else
            remoteView.setImageViewResource(R.id.play_pause_player_btn, R.drawable.ic_play_arrow_white_24dp)
        if (exoPlayer.repeatMode == Player.REPEAT_MODE_OFF)
            remoteView.setImageViewResource(R.id.replay_player_btn, R.drawable.ic_replay_white_24dp)
        else
            remoteView.setImageViewResource(R.id.replay_player_btn, R.drawable.ic_replay_error_24dp)

       /* val stopIntent = PendingIntent.getService(this, 0, Intent(this, VideoService::class.java).apply {
            putExtra(STOP_ACTION, 0) }, 0)
        remoteView!!.setOnClickPendingIntent(R.id.stop_player_btn, stopIntent)

        val closeIntent = PendingIntent.getService(this, 0, Intent(this, VideoService::class.java).apply {
            putExtra(CLOSE_ACTION, 1) }, 0)
        remoteView!!.setOnClickPendingIntent(R.id.kill_player_btn, closeIntent)

        val repeatIntent = PendingIntent.getService(this, 0, Intent(this, VideoService::class.java).apply {
            putExtra(REPEAT_ACTION, 0) }, 0)
        remoteView!!.setOnClickPendingIntent(R.id.replay_player_btn, repeatIntent)
        */
        val pauseIntent = PendingIntent.getService(this, 0, Intent(this, VideoService::class.java).apply {
            if (exoPlayer.playWhenReady) putExtra(PLAY_PAUSE_ACTION, 0)
            else putExtra(PLAY_PAUSE_ACTION, 1)
        }, 0)
        remoteView.setOnClickPendingIntent(R.id.play_pause_player_btn, pauseIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(this, "Default")
        notificationBuilder.setContent(remoteView)
        //TODO set app icon
        notificationBuilder.setSmallIcon(android.R.drawable.sym_def_app_icon)

        manager.createNotificationChannel(NotificationChannel("ID", "Main", NotificationManager.IMPORTANCE_DEFAULT))
        notificationBuilder.setChannelId("ID")
        val notification = notificationBuilder.build()
        startForeground(0, notification)
        manager.notify(NOTIFICAITON_ID, notification)
    }
}