package com.crmbl.thesafe.viewHolders

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.view.GestureDetector
import android.view.View
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.R
import com.crmbl.thesafe.listeners.ComposableGestureListener
import com.crmbl.thesafe.listeners.ComposableTouchListener
import com.crmbl.thesafe.utils.CryptoUtil
import kotlinx.android.synthetic.main.image_item.view.*
import kotlinx.coroutines.*
import pl.droidsonroids.gif.GifDrawable
import java.io.ByteArrayInputStream


class ImageViewHolder(itemView: View, private val _listener: ImageViewHolderListener): RecyclerView.ViewHolder(itemView) {

    interface ImageViewHolderListener {
        fun onDoubleTap(view: View, item: File)
    }

    private var imageListener : ImageViewHolderListener? = null
    private var gestureDetector : GestureDetector? = null
    private var touchListener : View.OnTouchListener? = null

    fun bind(file : File) {
        val splitedName = file.originName.split('.')
        itemView.textview_title.text = splitedName.first()
        itemView.textview_ext.text = splitedName.last()

        itemView.waiting_frame.visibility = View.VISIBLE
        itemView.waiting_frame.minimumHeight = file.height.toInt()
        itemView.waiting_frame.minimumWidth = file.width.toInt()
        val params = itemView.bottom_layout.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.BELOW, R.id.waiting_frame)
        itemView.bottom_layout.layoutParams = params

        imageListener = _listener
        gestureDetector = GestureDetector(itemView.context, ComposableGestureListener().onDoubleTap {
            imageListener!!.onDoubleTap(itemView, file); true})
        touchListener = ComposableTouchListener { _, event -> gestureDetector!!.onTouchEvent(event) ; true }
        itemView.imageView.setOnTouchListener(touchListener)

        var decryptedStream : ByteArrayInputStream? = null
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val deferred = async(Dispatchers.Default) {
                decryptedStream = CryptoUtil.decrypt(java.io.File(file.path))!!.inputStream()
            }

            deferred.await()
            params.addRule(RelativeLayout.BELOW, R.id.imageView)
            itemView.bottom_layout.layoutParams = params
            itemView.waiting_frame.visibility = View.GONE

            when {
                splitedName.last().toLowerCase() == "gif" -> {
                    itemView.imageView.setImageDrawable(GifDrawable(decryptedStream!!)) }
                else -> {
                    itemView.imageView.setImageDrawable(BitmapDrawable(Resources.getSystem(), decryptedStream!!)) }
            }
        }
    }

    fun clearAnimation() {
        if (itemView.imageView.drawable != null && itemView.imageView.drawable is GifDrawable)
            (itemView.imageView.drawable as GifDrawable).pause()

        itemView.clearAnimation()
    }

    fun resumeGif() {
        if (itemView.imageView.drawable != null && itemView.imageView.drawable is GifDrawable)
            (itemView.imageView.drawable as GifDrawable).start()
    }

    fun recycleView() {
        if (itemView.imageView.drawable == null) return

        val mediaViewDrawable = itemView.imageView.drawable
        if (mediaViewDrawable is GifDrawable)
            mediaViewDrawable.recycle()
        if (mediaViewDrawable is BitmapDrawable)
            mediaViewDrawable.bitmap.recycle()

        itemView.imageView.setImageDrawable(null)
        itemView.imageView.setOnTouchListener(null)
        gestureDetector = null
        imageListener = null
        touchListener = null
    }
}