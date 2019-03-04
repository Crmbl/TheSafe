package com.crmbl.thesafe.viewHolders

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.MainActivity
import com.crmbl.thesafe.R
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val textViewTitle : TextView = itemView.findViewById(R.id.textview_title)
    private val textViewExt : TextView = itemView.findViewById(R.id.textview_ext)
    private val mediaView : GifImageView = itemView.findViewById(R.id.imageView)
    private var drawable : Drawable? = null

    fun bind(file : File) {
        val splitedName = file.originName.split('.')
        textViewTitle.text = splitedName.first()
        textViewExt.text = splitedName.last()

        when {
            splitedName.last().toLowerCase() == "gif" -> {
                drawable = GifDrawable(file.decrypted!!)
                mediaView.setImageDrawable(drawable)
            }
            else -> {
                drawable = BitmapDrawable(Resources.getSystem(), file.decrypted!!.inputStream())
                mediaView.setImageDrawable(drawable)
            }
        }
    }

    fun clearAnimation() {
        if (drawable != null && drawable is GifDrawable)
            (drawable as GifDrawable).pause()

        itemView.clearAnimation()
    }

    fun resumeGif() {
        if (drawable != null && drawable is GifDrawable)
            (drawable as GifDrawable).start()
    }

    fun recycleView(activity: MainActivity) {
        if (drawable == null) return

        activity.runOnUiThread { mediaView.setImageDrawable(null) }
        if (drawable is GifDrawable)
            (drawable as GifDrawable).recycle()
        if (drawable is BitmapDrawable)
            (drawable as BitmapDrawable).bitmap.recycle()

        drawable = null
    }
}