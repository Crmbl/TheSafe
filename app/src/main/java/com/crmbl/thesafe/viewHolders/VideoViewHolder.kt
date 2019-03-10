package com.crmbl.thesafe.viewHolders

import android.annotation.SuppressLint
import android.os.Handler
import android.view.GestureDetector
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.R
import com.crmbl.thesafe.listeners.*
import com.crmbl.thesafe.utils.CryptoUtil
import com.crmbl.thesafe.utils.UriByteDataHelper
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import kotlinx.android.synthetic.main.video_item.view.*
import kotlinx.coroutines.*
import java.io.IOException


@Suppress("DEPRECATION")
class VideoViewHolder(itemView: View, private val _listener: VideoViewHolderListener): RecyclerView.ViewHolder(itemView) {

    interface VideoViewHolderListener {
        fun onFullScreenButtonClick(view: View, item: File)
    }

    private var isRecycling: Boolean = false
    private var gestureDetector: GestureDetector? = null
    private var videoListener: VideoViewHolderListener? = null
    private var updateHandler: Handler? = null
    private var updateProgressAction: java.lang.Runnable? = null
    private var touchListener: View.OnTouchListener? = null
    private var listener: Player.EventListener? = null
    private var progressListener: TimeBar.OnScrubListener? = null

    @SuppressLint("ClickableViewAccessibility")
    fun bind(file : File) {
        //region init vars

        val splitedName = file.originName.split('.')
        itemView.textview_title.text = splitedName.first()
        itemView.textview_ext.text = splitedName.last()

        itemView.waiting_frame.visibility = View.VISIBLE
        itemView.waiting_frame.minimumHeight = file.height.toInt()
        itemView.waiting_frame.minimumWidth = file.width.toInt()
        itemView.controller_layout.minimumHeight = file.height.toInt()
        itemView.controller_layout.minimumWidth = file.width.toInt()
        val params = itemView.bottom_layout.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.BELOW, R.id.waiting_frame)
        itemView.bottom_layout.layoutParams = params

        val player: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(itemView.context, DefaultTrackSelector())
        player.playWhenReady = false
        player.volume = 0f
        player.repeatMode = Player.REPEAT_MODE_ALL

        itemView.videoView.player = player

        //endregion init vars

        //region init listeners

        player.addVideoListener(ComposableVideoListener().onVideoSizeChanged { _, _, _, _ -> onVideoSizeChanged() })

        videoListener = _listener
        itemView.controller_pause.setOnClickListener { pauseVideo() }
        itemView.controller_play.setOnClickListener { playVideo() }
        itemView.controller_fullscreen.setOnClickListener {
            pauseVideo()
            videoListener!!.onFullScreenButtonClick(itemView, file)
        }

        gestureDetector = GestureDetector(itemView.context, ComposableGestureListener().onDoubleTap { toggleController() })
        touchListener = ComposableTouchListener { _, event -> gestureDetector!!.onTouchEvent(event) ; true }
        itemView.videoView.setOnTouchListener(touchListener)

        updateHandler = Handler()
        updateProgressAction = Runnable { updateProgressBar() }
        listener = ComposablePlayerEventListener().onPlayerStateChanged { _, _ -> updateProgressBar() }
        itemView.videoView.player.addListener(listener)

        progressListener = ComposableTimeBarScrubListener(onStop = { _, position, _ -> seekVideo(position) })
        itemView.controller_progress.addListener(progressListener)

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
            (itemView.videoView.player as SimpleExoPlayer).prepare(mediaSource)
        }

        //endregion init player
    }

    private fun pauseVideo() {
        itemView.controller_pause.visibility = View.GONE
        itemView.controller_play.visibility = View.VISIBLE
        itemView.videoView.player?.playWhenReady = false
        updateHandler?.removeCallbacks(updateProgressAction)
    }

    private fun playVideo() {
        itemView.controller_play.visibility = View.GONE
        itemView.controller_pause.visibility = View.VISIBLE
        hideController()
        itemView.videoView.player?.playWhenReady = true
    }

    private fun showController() {
        itemView.controller_layout.clearAnimation()
        itemView.controller_layout.visibility = View.VISIBLE
    }

    private fun hideController() {
        if (itemView.controller_layout.visibility == View.VISIBLE)
            itemView.controller_layout.visibility = View.INVISIBLE
    }

    private fun toggleController(): Boolean {
        if (itemView.controller_layout.visibility == View.VISIBLE)
            hideController()
        else
            showController()
        return true
    }

    private fun onVideoSizeChanged() {
        itemView.waiting_frame.visibility = View.GONE
        val params = itemView.bottom_layout.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.BELOW, R.id.videoView)
        itemView.bottom_layout.layoutParams = params
    }

    private fun updateProgressBar() {
        val duration = (if (itemView.videoView.player == null) 0
                        else itemView.videoView.player!!.duration).toLong()
        val position = (if (itemView.videoView.player == null) 0
                        else itemView.videoView.player?.currentPosition)!!.toLong()
        val bufferedPosition = (if (itemView.videoView.player == null) 0
                                else itemView.videoView.player?.bufferedPosition)!!.toLong()

        itemView.controller_progress.setPosition(position)
        itemView.controller_progress.setBufferedPosition(bufferedPosition)
        itemView.controller_progress.setDuration(duration)

        updateHandler?.removeCallbacks(updateProgressAction)
        val playbackState = if (itemView.videoView.player == null) Player.STATE_IDLE
                            else itemView.videoView.player?.playbackState
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            var delayMs: Long
            if (itemView.videoView.player?.playWhenReady!! && playbackState == Player.STATE_READY) {
                delayMs = 800 - position % 800
                if (delayMs < 200) delayMs += 800
            } else delayMs = 800
            updateHandler?.postDelayed(updateProgressAction, delayMs)
        }
    }

    private fun seekVideo(position: Long) {
        (itemView.videoView.player as SimpleExoPlayer).seekTo(position)
    }

    fun clearAnimation() {
        if (!isRecycling) pauseVideo()
        itemView.clearAnimation()
    }

    fun resumeVideo() {
        playVideo()
    }

    fun recycleView() {
        isRecycling = true
        (itemView.videoView.player as SimpleExoPlayer).release()
        (itemView.videoView.player as SimpleExoPlayer).setVideoListener(null)
        (itemView.videoView.player as SimpleExoPlayer).removeListener(listener)
        itemView.controller_pause.setOnClickListener(null)
        itemView.controller_play.setOnClickListener(null)
        itemView.controller_fullscreen.setOnClickListener(null)
        itemView.videoView.setOnTouchListener(null)
        itemView.controller_progress.removeListener(progressListener)
        updateHandler?.removeCallbacks(updateProgressAction)

        progressListener = null
        listener = null
        touchListener = null
        updateHandler = null
        updateProgressAction = null
        videoListener = null
        gestureDetector = null
    }
}

