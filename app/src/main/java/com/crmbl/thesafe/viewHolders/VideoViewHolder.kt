package com.crmbl.thesafe.viewHolders

import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.MainActivity
import com.crmbl.thesafe.R
import com.crmbl.thesafe.listeners.ComposableVideoListener
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
import java.io.IOException


@Suppress("DEPRECATION")
class VideoViewHolder(itemView: View/*, private val activity: MainActivity?*/): RecyclerView.ViewHolder(itemView) {

    private var isRecycling: Boolean = false

    fun bind(file : File, mRecyclerView: RecyclerView?) {
        val splitedName = file.originName.split('.')
        itemView.findViewById<TextView>(R.id.textview_title).text = splitedName.first()
        itemView.findViewById<TextView>(R.id.textview_ext).text = splitedName.last()
        itemView.findViewById<FrameLayout>(R.id.waiting_frame).visibility = View.VISIBLE

        val bottomLayout = itemView.findViewById<LinearLayout>(R.id.bottom_layout)
        val params = bottomLayout.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.BELOW, R.id.waiting_frame)
        bottomLayout.layoutParams = params

        val videoView = itemView.findViewById<PlayerView>(R.id.videoView)
        val player: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(mRecyclerView?.context, DefaultTrackSelector())
        player.addVideoListener(ComposableVideoListener().onVideoSizeChanged { _, _, _, _ -> onVideoSizeChanged() })
        player.playWhenReady = false
        player.volume = 0f
        player.repeatMode = Player.REPEAT_MODE_ALL

        videoView.player = player
        videoView.exo_fullscreen.setOnClickListener { v ->
            videoView.exo_pause.performClick()
            //TODO remove the activity param, awwwwful
            //activity?.showPopup(v!!, 0, file)
        }

        //TODO improve behavior of controller, show on double tap like fullscreen
        //TODO remove layout_height from waiting_frame
        //TODO won't work for video : BitmapFactory.decode
        /*val scale = activity!!.resources.displayMetrics.density
        val ratio: Float = videoView.width.toFloat() / file.width.toFloat()
        val tHeight = file.height * ratio * scale
        videoView.minimumHeight = tHeight.toInt()
        itemView.findViewById<FrameLayout>(R.id.waiting_frame).minimumHeight = tHeight.toInt()
        //videoView.minimumWidth = file.width

        //android.util.Log.d("VideoHolder", "scale: $scale // ratio: $ratio // tHeight: $tHeight // fileHeight: ${file.height} // fileWidth: ${file.width}")
        android.util.Log.d("TEST : VideoView", "Width: ${videoView.width} // Height: ${videoView.height}")
        android.util.Log.d("TEST : File", "Width: ${file.width} // Height: ${file.height}")*/

        val byteArrayDataSource = ByteArrayDataSource(file.decrypted!!)
        val mediaByteUri = UriByteDataHelper().getUri(file.decrypted!!)
        val dataSpec = DataSpec(mediaByteUri)
        try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
        val factory = object : DataSource.Factory { override fun createDataSource(): DataSource { return byteArrayDataSource } }
        val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)

        player.prepare(mediaSource)
        videoView.hideController()
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

