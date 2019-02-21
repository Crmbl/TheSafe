package com.crmbl.thesafe

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import androidx.recyclerview.widget.LinearLayoutManager


class ItemAdapter(private val context: Context, private val dataSource : MutableList<File>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
    class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        private val textViewTitle : TextView = itemView.findViewById(R.id.textview_title)
        private val textViewExt : TextView = itemView.findViewById(R.id.textview_ext)
        private val mediaView : GifImageView = itemView.findViewById(R.id.imageView) as GifImageView

        fun bind(file : File) {
            val splitedName = file.originName.split('.')
            textViewTitle.text = splitedName.first()
            textViewExt.text = splitedName.last()
            mediaView.setImageDrawable(GifDrawable(file.decrypted!!))
        }

        fun clearAnimation() {
            itemView.clearAnimation()
        }
    }

    //endregion

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
                return ItemViewHolder(view)
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
                holder.bind(file)
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
        super.onViewDetachedFromWindow(holder)
        if (holder is ItemViewHolder)
            holder.clearAnimation()
    }

    private fun setAnimation(itemView: View, position: Int) {
        if (position > lastPosition) {
            val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down)
            itemView.startAnimation(animation)
            lastPosition = position
        }
    }
}