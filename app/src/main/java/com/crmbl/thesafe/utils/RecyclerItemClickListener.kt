package com.crmbl.thesafe.utils

import android.content.Context
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import android.view.GestureDetector
import android.view.View
import com.crmbl.thesafe.R
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.button.MaterialButton


class RecyclerItemClickListener(context: Context, recyclerView: RecyclerView, private val mListener: OnItemClickListener?)
            : RecyclerView.OnItemTouchListener {

    private var mGestureDetector: GestureDetector

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
        fun onLongItemClick(view: View?, position: Int)
    }

    init {
        mGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val child = recyclerView.findChildViewUnder(e.x, e.y)
                if (child != null && mListener != null) {
                    mListener.onLongItemClick(child, recyclerView.getChildAdapterPosition(child))
                }
            }
        })
    }

    override fun onInterceptTouchEvent(view: RecyclerView, e: MotionEvent): Boolean {
        val childView = view.findChildViewUnder(e.x, e.y)
        val isScrollUpView = childView?.findViewById<MaterialButton>(R.id.button_scrollUp) != null
        val isVideoView = childView?.findViewById<PlayerView>(R.id.videoView) != null

        if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e)
            && !isScrollUpView && !isVideoView) {
            mListener.onItemClick(childView, view.getChildAdapterPosition(childView))
            return true
        }
        return false
    }

    override fun onTouchEvent(view: RecyclerView, motionEvent: MotionEvent) {}

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}