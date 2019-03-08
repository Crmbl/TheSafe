package com.crmbl.thesafe.viewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.scrollup_item.view.*


class ScrollUpViewHolder(itemView: View, private val _listener: ScrollUpViewHolderListener): RecyclerView.ViewHolder(itemView) {

    interface ScrollUpViewHolderListener {
        fun onClick()
    }

    private var scrollUpListener: ScrollUpViewHolderListener? = null

    fun bind() {
        scrollUpListener = _listener
        itemView.button_scrollUp.setOnClickListener {
            scrollUpListener?.onClick()
        }
    }

    fun recycleView() {
        itemView.button_scrollUp.setOnClickListener(null)
        scrollUpListener = null
    }
}