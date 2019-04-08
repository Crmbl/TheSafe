package com.crmbl.thesafe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.viewHolders.*


class ItemAdapter(
    private val dataSource : MutableList<File>,
    private val videoListener: VideoViewHolder.VideoViewHolderListener,
    private val imageListener: ImageViewHolder.ImageViewHolderListener,
    private val soundListener: SoundViewHolder.SoundViewHolderListener,
    private val scrollUpListener: ScrollUpViewHolder.ScrollUpViewHolderListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val headerView = 0
    private val imageView = 1
    private val videoView = 2
    private val soundView = 3
    private val footerView = 4
    private val scrollUpView = 5
    private var lastPosition = -1

    //region override methods

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            headerView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.header_item, parent, false)
                return HeaderViewHolder(view)
            }
            imageView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.image_item, parent, false)
                    .apply { clipToOutline = true }

                return ImageViewHolder(view, imageListener)
            }
            videoView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.video_item, parent, false)
                    .apply { clipToOutline = true }

                return VideoViewHolder(view, videoListener)
            }
            soundView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.sound_item, parent, false)
                    .apply { clipToOutline = true }

                return SoundViewHolder(view, soundListener)
            }
            footerView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.footer_item, parent, false)
                return FooterViewHolder(view)
            }
            scrollUpView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.scrollup_item, parent, false)
                return ScrollUpViewHolder(view, scrollUpListener)
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
        if (dataSource[position].type == "imageView")
            return imageView
        if (dataSource[position].type == "videoView")
            return videoView
        if (dataSource[position].type == "soundView")
            return soundView

        throw Exception("Did not find a suitable view for the item/position")
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageViewHolder -> {
                val file : File = dataSource[position]
                holder.bind(file)
                setAnimation(holder.itemView, position)
            }
            is VideoViewHolder -> {
                val file : File = dataSource[position]
                holder.bind(file)
                setAnimation(holder.itemView, position)
            }
            is SoundViewHolder -> {
                val file: File = dataSource[position]
                holder.bind(file)
                setAnimation(holder.itemView, position)
            }
            is FooterViewHolder -> {
                setAnimation(holder.itemView, position)
            }
            is ScrollUpViewHolder -> {
                holder.bind()
                setAnimation(holder.itemView, position)
            }
            is HeaderViewHolder -> {}
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (holder is VideoViewHolder)
            holder.resumeVideo()
        if (holder is ImageViewHolder)
            holder.resumeGif()

        super.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ImageViewHolder)
            holder.clearAnimation()
        if (holder is VideoViewHolder)
            holder.clearAnimation()
        if (holder is SoundViewHolder)
            holder.clearAnimation()

        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is VideoViewHolder)
            holder.recycleView()
        if (holder is ImageViewHolder)
            holder.recycleView()
        if (holder is ScrollUpViewHolder)
            holder.recycleView()
        if (holder is SoundViewHolder)
            holder.recycleView()

        super.onViewRecycled(holder)
    }

    //endregion override methods

    private fun setAnimation(itemView: View, position: Int) {
        if (position > lastPosition) {
            val animation: Animation = AnimationUtils.loadAnimation(itemView.context, R.anim.item_animation_fall_down)
            itemView.startAnimation(animation)
            lastPosition = position
        }
    }
}