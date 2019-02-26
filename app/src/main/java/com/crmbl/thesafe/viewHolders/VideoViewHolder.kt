package com.crmbl.thesafe.viewHolders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.MainActivity
import com.crmbl.thesafe.R
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


class VideoViewHolder(itemView: View, private val activity: MainActivity?): RecyclerView.ViewHolder(itemView) {

    private val textViewTitle : TextView = itemView.findViewById(R.id.textview_title)
    private val textViewExt : TextView = itemView.findViewById(R.id.textview_ext)
    private val videoView : PlayerView = itemView.findViewById(R.id.videoView)
    private var player: SimpleExoPlayer? = null

    fun bind(file : File, mRecyclerView: RecyclerView?) {
        val splitedName = file.originName.split('.')
        textViewTitle.text = splitedName.first()
        textViewExt.text = splitedName.last()

        player = ExoPlayerFactory.newSimpleInstance(mRecyclerView?.context, DefaultTrackSelector())
        videoView.player = player
        videoView.exo_fullscreen.setOnClickListener { v ->
            videoView.exo_pause.performClick()
            activity?.showPopup(v!!, 0, file)
        }

        val byteArrayDataSource = ByteArrayDataSource(file.decrypted!!)
        val mediaByteUri = UriByteDataHelper().getUri(file.decrypted!!)
        val dataSpec = DataSpec(mediaByteUri)
        try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
        val factory = object : DataSource.Factory { override fun createDataSource(): DataSource { return byteArrayDataSource } }
        val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)

        player?.prepare(mediaSource)
        player?.playWhenReady = true
        player?.volume = 0f
        player?.repeatMode = Player.REPEAT_MODE_ALL
        videoView.hideController()
    }

    fun clearAnimation() {
        videoView.exo_pause.performClick()
        itemView.clearAnimation()
    }

    fun resumeVideo() {
        videoView.exo_play.performClick()
    }
}

