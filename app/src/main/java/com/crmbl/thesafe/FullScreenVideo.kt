package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.*
import android.widget.*
import com.crmbl.thesafe.utils.UriByteDataHelper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.exo_controller_fullscreen.view.*
import java.io.IOException


@SuppressLint("ClickableViewAccessibility", "InflateParams")
class FullScreenVideo(internal var mContext: Context, v: View, imageBytes: ByteArray) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
        R.layout.video_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    internal var view: View = contentView
    internal var loading: ProgressBar
    internal var videoView: ZoomableExoPlayerView
    private var lockLayout: FrameLayout
    private var player: SimpleExoPlayer? = null

    init {
        isOutsideTouchable = true
        isFocusable = true
        elevation = 5.0f

        val closeButton = this.view.findViewById(R.id.ib_close) as ImageButton
        lockLayout = view.findViewById(R.id.layout_lock)
        videoView = view.findViewById(R.id.video)
        loading = view.findViewById(R.id.loading)
        loading.isIndeterminate = true
        loading.visibility = View.VISIBLE

        player = ExoPlayerFactory.newSimpleInstance(mContext, DefaultTrackSelector())
        player?.volume = 1f
        player?.playWhenReady = true
        videoView.player = player

        //region listeners

        closeButton.setOnClickListener { dismiss() }

        player?.addVideoListener(object: VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {}
            override fun onRenderedFirstFrame() {
                loading.setBackgroundColor(mContext.getColor(R.color.colorItemBackground))
                loading.visibility = View.GONE
                videoView.showController()
            }
        })

        videoView.exo_quit_fullscreen.setOnClickListener { this.dismiss() }
        videoView.exo_mute.setOnClickListener {
            player?.volume = 1f
            videoView.exo_mute.visibility = View.GONE
            videoView.exo_volume.visibility = View.VISIBLE
        }
        videoView.exo_volume.setOnClickListener {
            player?.volume = 0f
            videoView.exo_mute.visibility = View.VISIBLE
            videoView.exo_volume.visibility = View.GONE
        }
        //endregion

        val byteArrayDataSource = ByteArrayDataSource(imageBytes)
        val mediaByteUri = UriByteDataHelper().getUri(imageBytes)
        val dataSpec = DataSpec(mediaByteUri)

        try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
        val factory = object : com.google.android.exoplayer2.upstream.DataSource.Factory {
            override fun createDataSource(): com.google.android.exoplayer2.upstream.DataSource { return byteArrayDataSource } }

        val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)
        player?.prepare(mediaSource)

        showAtLocation(v, Gravity.CENTER, 0, 0)
    }

    override fun dismiss() {
        player?.release()
        super.dismiss()
    }

    fun onPause() {
        videoView.exo_pause.performClick()
        lockLayout.visibility = View.VISIBLE
    }

    fun onResume() {
        videoView.exo_play.performClick()
        lockLayout.visibility = View.GONE
    }
}