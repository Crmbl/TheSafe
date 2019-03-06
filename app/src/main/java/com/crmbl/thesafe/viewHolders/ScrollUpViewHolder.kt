package com.crmbl.thesafe.viewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.R
import com.google.android.material.button.MaterialButton
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller


class ScrollUpViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val button : MaterialButton = itemView.findViewById(R.id.button_scrollUp)

    fun bind(parent: RecyclerView?) {
        button.setOnClickListener {
            parent?.layoutManager!!.scrollToPosition(0)
            parent.tag = "smoothScrolling"
        }
    }
}