package com.crmbl.thesafe.viewHolders

import android.content.Context
import android.view.GestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.R
import com.crmbl.thesafe.listeners.ComposableGestureListener
import com.crmbl.thesafe.listeners.ComposableTouchListener
import com.crmbl.thesafe.listeners.ComposableVideoListener
import com.crmbl.thesafe.utils.CryptoUtil
import com.crmbl.thesafe.utils.UriByteDataHelper
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import kotlinx.android.synthetic.main.exo_controller.view.*
import kotlinx.coroutines.*
import java.io.IOException


//TODO improve behavior of controller, show on double tap like fullscreen
@Suppress("DEPRECATION")
class VideoViewHolder(itemView: View, listener: VideoViewHolderListener): RecyclerView.ViewHolder(itemView) {

    interface VideoViewHolderListener {
        fun onFullScreenButtonClick(item: File)
    }

    private var videoListener: VideoViewHolderListener = listener
    private var isRecycling: Boolean = false

    fun bind(file : File, mRecyclerView: RecyclerView?) {
        //region init vars

        val splitedName = file.originName.split('.')
        itemView.findViewById<TextView>(R.id.textview_title).text = splitedName.first()
        itemView.findViewById<TextView>(R.id.textview_ext).text = splitedName.last()

        val waitingFrame = itemView.findViewById<FrameLayout>(R.id.waiting_frame)
        waitingFrame.visibility = View.VISIBLE
        waitingFrame.minimumHeight = file.height.toInt()
        waitingFrame.minimumWidth = file.width.toInt()
        val bottomLayout = itemView.findViewById<LinearLayout>(R.id.bottom_layout)
        val params = bottomLayout.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.BELOW, R.id.waiting_frame)
        bottomLayout.layoutParams = params

        val videoView = itemView.findViewById<PlayerView>(R.id.videoView)
        val player: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(mRecyclerView?.context, DefaultTrackSelector())
        player.playWhenReady = false
        player.volume = 0f
        player.repeatMode = Player.REPEAT_MODE_ALL

        videoView.player = player
        videoView.controllerAutoShow = false
        videoView.controllerHideOnTouch = false
        videoView.hideController()

        //endregion init vars

        //region init listeners

        //TODO none of this work, total bullshit
        player.addVideoListener(ComposableVideoListener().onVideoSizeChanged { _, _, _, _ -> onVideoSizeChanged() })
        videoView.exo_fullscreen.setOnClickListener {
            videoView.exo_pause.performClick()
            videoListener.onFullScreenButtonClick(file)
        }
        val gestureDetector = GestureDetector(mRecyclerView?.context, ComposableGestureListener().onDoubleTap {
            toggleController(); true
        }.onSingleTapUp { false })
        videoView.setOnTouchListener(ComposableTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        })

        //endregion init listeners

        //region init player

        var decrypted : ByteArray? = null
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val deferred = async(Dispatchers.Default) {
                decrypted = CryptoUtil.decrypt(java.io.File(file.path))
            }

            deferred.await()
            val byteArrayDataSource = ByteArrayDataSource(decrypted)
            val mediaByteUri = UriByteDataHelper().getUri(decrypted!!)
            val dataSpec = DataSpec(mediaByteUri)
            try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
            val factory = object : DataSource.Factory { override fun createDataSource(): DataSource { return byteArrayDataSource } }
            val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)

            val view = itemView.findViewById<PlayerView>(R.id.videoView)
            (view.player as SimpleExoPlayer).prepare(mediaSource)
        }

        //endregion init player
    }

    private fun toggleController() {
        val videoView = itemView.findViewById<PlayerView>(R.id.videoView)
        if (videoView.exo_fullscreen.visibility == View.VISIBLE) videoView.hideController()
        else videoView.showController()
    }

    private fun onVideoSizeChanged() {
        itemView.findViewById<FrameLayout>(R.id.waiting_frame).visibility = View.GONE

        val bottomLayout = itemView.findViewById<LinearLayout>(R.id.bottom_layout)
        val params = bottomLayout.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.BELOW, R.id.videoView)
        bottomLayout.layoutParams = params
    }

    fun clearAnimation() {
        if (!isRecycling)
            (itemView.findViewById<PlayerView>(R.id.videoView).player as SimpleExoPlayer).playWhenReady = false

        itemView.clearAnimation()
    }

    fun resumeVideo() {
        (itemView.findViewById<PlayerView>(R.id.videoView).player as SimpleExoPlayer).playWhenReady = true
    }

    fun recycleView() {
        isRecycling = true

        val mediaView = itemView.findViewById<PlayerView>(R.id.videoView)
        (mediaView.player as SimpleExoPlayer).release()
        (mediaView.player as SimpleExoPlayer).setVideoListener(null)
        mediaView.exo_fullscreen.setOnClickListener(null)
    }
}

