package com.crmbl.thesafe.viewHolders

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.MainActivity
import com.crmbl.thesafe.R
import com.crmbl.thesafe.utils.CryptoUtil
import kotlinx.coroutines.*
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import java.io.ByteArrayInputStream


class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    fun bind(file : File) {
        val splitedName = file.originName.split('.')
        itemView.findViewById<TextView>(R.id.textview_title).text = splitedName.first()
        itemView.findViewById<TextView>(R.id.textview_ext).text = splitedName.last()

        val waitingFrame = itemView.findViewById<FrameLayout>(R.id.waiting_frame)
        waitingFrame.visibility = View.VISIBLE
        waitingFrame.minimumHeight = file.height.toInt()
        waitingFrame.minimumWidth = file.width.toInt()
        val bottomLayout = itemView.findViewById<LinearLayout>(R.id.bottom_layout)
        val params = bottomLayout.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.BELOW, R.id.waiting_frame)
        bottomLayout.layoutParams = params

        var decryptedStream : ByteArrayInputStream? = null
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val deferred = async(Dispatchers.Default) {
                decryptedStream = CryptoUtil.decrypt(java.io.File(file.path))!!.inputStream()
            }

            deferred.await()
            params.addRule(RelativeLayout.BELOW, R.id.imageView)
            bottomLayout.layoutParams = params
            waitingFrame.visibility = View.GONE

            when {
                splitedName.last().toLowerCase() == "gif" -> {
                    itemView.findViewById<GifImageView>(R.id.imageView).setImageDrawable(
                        GifDrawable(decryptedStream!!))
                }
                else -> {
                    itemView.findViewById<GifImageView>(R.id.imageView).setImageDrawable(
                        BitmapDrawable(Resources.getSystem(), decryptedStream!!))
                }
            }
        }
    }

    fun clearAnimation() {
        val mediaView = itemView.findViewById<GifImageView>(R.id.imageView)
        if (mediaView.drawable != null && mediaView.drawable is GifDrawable)
            (mediaView.drawable as GifDrawable).pause()

        itemView.clearAnimation()
    }

    fun resumeGif() {
        val mediaView = itemView.findViewById<GifImageView>(R.id.imageView)
        if (mediaView.drawable != null && mediaView.drawable is GifDrawable)
            (mediaView.drawable as GifDrawable).start()
    }

    fun recycleView() {
        val mediaView = itemView.findViewById<GifImageView>(R.id.imageView)
        if (mediaView.drawable == null) return

        val mediaViewDrawable = mediaView.drawable
        if (mediaViewDrawable is GifDrawable)
            mediaViewDrawable.recycle()
        if (mediaViewDrawable is BitmapDrawable)
            mediaViewDrawable.bitmap.recycle()

        mediaView.setImageDrawable(null)
    }
}