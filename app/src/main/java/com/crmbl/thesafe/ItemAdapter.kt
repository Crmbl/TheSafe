package com.crmbl.thesafe

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.button.MaterialButton
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player.REPEAT_MODE_ALL
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.*
import kotlinx.android.synthetic.main.exo_controller.view.*
import java.io.IOException


class ItemAdapter(private val context: Context, private val dataSource : MutableList<File>, private val activity: MainActivity?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val headerView = 0
    private val itemView = 1
    private val footerView = 2
    private val scrollUpView = 3
    private var lastPosition = -1
    private var mRecyclerView: RecyclerView? = null

    //region ViewHolders

    class FooterViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
    class HeaderViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
    class ScrollUpViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        private val button : MaterialButton = itemView.findViewById(R.id.button_scrollUp)

        fun bind(parent: RecyclerView?) {
            button.setOnClickListener {
                parent!!.smoothScrollToPosition(0)
                parent.tag = "smoothScrolling"
            }
        }
    }

    class ItemViewHolder(itemView: View, private val activity: MainActivity?): RecyclerView.ViewHolder(itemView) {

        private val textViewTitle : TextView = itemView.findViewById(R.id.textview_title)
        private val textViewExt : TextView = itemView.findViewById(R.id.textview_ext)
        private val bottomLayout : LinearLayout = itemView.findViewById(R.id.bottom_layout)
        private val mediaView : GifImageView = itemView.findViewById(R.id.imageView)
        private val videoView : PlayerView = itemView.findViewById(R.id.videoView)
        private var player: SimpleExoPlayer? = null

        fun bind(file : File, mRecyclerView: RecyclerView?) {
            val splitedName = file.originName.split('.')
            textViewTitle.text = splitedName.first()
            textViewExt.text = splitedName.last()

            val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val imageFileExtensions: Array<String> = arrayOf("png", "jpg", "jpeg", "bmp", "pdf")
            when {
                splitedName.last().toLowerCase() == "gif" -> {
                    if (player != null) {
                        videoView.visibility = View.GONE
                        player?.release()
                    }

                    params.addRule(RelativeLayout.BELOW, R.id.imageView)
                    mediaView.visibility = View.VISIBLE
                    mediaView.setImageDrawable(GifDrawable(file.decrypted!!))
                }
                imageFileExtensions.contains(splitedName.last().toLowerCase()) -> {
                    if (player != null) {
                        videoView.visibility = View.GONE
                        player?.release()
                    }

                    params.addRule(RelativeLayout.BELOW, R.id.imageView)
                    mediaView.visibility = View.VISIBLE
                    mediaView.setImageDrawable(BitmapDrawable(Resources.getSystem(), file.decrypted!!.inputStream()))
                }
                file.originName.isNotEmpty() -> {
                    if (mediaView.visibility != View.GONE) {
                        mediaView.visibility = View.GONE
                    }

                    params.addRule(RelativeLayout.BELOW, R.id.videoView)
                    videoView.visibility = View.VISIBLE
                    playVideo(file, mRecyclerView!!)
                }
            }
            bottomLayout.layoutParams = params
        }

        private fun playVideo(file: File, parent: RecyclerView) {
            player = ExoPlayerFactory.newSimpleInstance(parent.context, DefaultTrackSelector())
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

            //val mediaSource = ExtractorMediaSource.Factory(DefaultDataSourceFactory(parent.context, "TheSafe")).createMediaSource(Uri.parse(file.path))

            player?.prepare(mediaSource)
            player?.playWhenReady = true
            player?.volume = 0f
            player?.repeatMode = REPEAT_MODE_ALL
            videoView.hideController()
        }

        fun clearAnimation() {
            if (videoView.visibility != View.GONE)
                videoView.exo_pause.performClick()
            itemView.clearAnimation()
        }

        fun resumeVideo() {
            if (videoView.visibility != View.GONE)
                videoView.exo_play.performClick()
        }
    }

    //endregion

    //region Override

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            headerView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item_header, parent, false)
                return HeaderViewHolder(view)
            }
            itemView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
                view.clipToOutline = true
                return ItemViewHolder(view, activity)
            }
            footerView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item_footer, parent, false)
                return FooterViewHolder(view)
            }
            scrollUpView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item_scrollup, parent, false)
                return ScrollUpViewHolder(view)
            }
        }

        throw Exception("This viewType has not been defined : $viewType")
    }

    override fun getItemViewType(position: Int): Int {
        if (dataSource[position].type == "header")
            return headerView
        if (dataSource[position].type == "footer")
            return footerView
        if (dataSource[position].type == "scrollUp")
            return scrollUpView

        return itemView
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                val file : File = dataSource[position]
                holder.bind(file, mRecyclerView)
                setAnimation(holder.itemView, position)
            }
            is FooterViewHolder -> {
                setAnimation(holder.itemView, position)
            }
            is ScrollUpViewHolder -> {
                holder.bind(mRecyclerView)
                setAnimation(holder.itemView, position)
            }
            is HeaderViewHolder -> {}
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ItemViewHolder)
            holder.clearAnimation()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ItemViewHolder)
            holder.resumeVideo()
        super.onViewAttachedToWindow(holder)
    }

    //endregion Override

    private fun setAnimation(itemView: View, position: Int) {
        if (position > lastPosition) {
            val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down)
            itemView.startAnimation(animation)
            lastPosition = position
        }
    }
}