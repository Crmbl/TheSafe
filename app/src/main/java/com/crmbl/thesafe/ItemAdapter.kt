package com.crmbl.thesafe

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView

class ItemAdapter(private val context: Context, private val dataSource : MutableList<File>) : RecyclerView.Adapter<ItemViewHolder>() {

    private var lastPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        view.clipToOutline = true
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val file : File = dataSource[position]
        holder.bind(file)
        setAnimation(holder.itemView, position)
    }

    override fun onViewDetachedFromWindow(holder: ItemViewHolder) {
        super.onViewDetachedFromWindow(holder)
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