package com.crmbl.thesafe.utils

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import java.io.IOException

class VideoService: Service() {
    private lateinit var exoPlayer: SimpleExoPlayer

    override fun onBind(intent: Intent?): IBinder {
        intent?.let {
            exoPlayer.playWhenReady = true
            loadMedia()
        }
        return VideoServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector())
    }

    inner class VideoServiceBinder : Binder() {
        fun getExoPlayerInstance() = exoPlayer
    }

    private fun loadMedia() {
        /*val byteArrayDataSource = ByteArrayDataSource(bytes)
        val mediaByteUri = UriByteDataHelper().getUri(bytes!!)
        val dataSpec = DataSpec(mediaByteUri)
        try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
        val factory = object : DataSource.Factory { override fun createDataSource(): DataSource { return byteArrayDataSource } }
        val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)
        exoPlayer.prepare(mediaSource)
        bytes = null*/
    }
}