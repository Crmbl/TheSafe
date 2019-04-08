package com.crmbl.thesafe.viewHolders

import android.view.GestureDetector
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.listeners.ComposableGestureListener
import com.crmbl.thesafe.listeners.ComposableTouchListener
import kotlinx.android.synthetic.main.sound_item.view.*


class SoundViewHolder(itemView: View, private val _listener: SoundViewHolderListener):
    RecyclerView.ViewHolder(itemView) {

    interface SoundViewHolderListener {
        fun onDoubleTap(view: View, item: File)
    }

    private var soundListener: SoundViewHolderListener? = null
    private var gestureDetector : GestureDetector? = null
    private var touchListener : View.OnTouchListener? = null

    fun bind(file: File) {
        val splitedName = file.originName.split('.')
        itemView.textview_title.text = splitedName.first()
        itemView.textview_ext.text = splitedName.last()

        soundListener = _listener
        gestureDetector = GestureDetector(itemView.context, ComposableGestureListener().onDoubleTap {
            soundListener!!.onDoubleTap(itemView, file); true})
        touchListener = ComposableTouchListener { _, event -> gestureDetector!!.onTouchEvent(event) ; true }
        itemView.soundView.setOnTouchListener(touchListener)
    }

    fun clearAnimation() {
        itemView.clearAnimation()
    }

    fun recycleView() {
        gestureDetector = null
        soundListener = null
        touchListener = null
    }
}