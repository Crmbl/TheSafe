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
            val linearSmoothScroller = object : LinearSmoothScroller(parent?.context) {
                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return 18f / displayMetrics.densityDpi
                }
            }
            linearSmoothScroller.targetPosition = 0
            parent?.layoutManager!!.startSmoothScroll(linearSmoothScroller)
            parent.tag = "smoothScrolling"
        }
    }
}