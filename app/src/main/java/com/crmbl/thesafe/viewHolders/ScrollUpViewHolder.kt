package com.crmbl.thesafe.viewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.R
import com.google.android.material.button.MaterialButton

class ScrollUpViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val button : MaterialButton = itemView.findViewById(R.id.button_scrollUp)

    fun bind(parent: RecyclerView?) {
        button.setOnClickListener {
            parent!!.smoothScrollToPosition(0)
            parent.tag = "smoothScrolling"
        }
    }
}