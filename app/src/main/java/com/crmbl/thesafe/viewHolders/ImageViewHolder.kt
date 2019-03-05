package com.crmbl.thesafe.viewHolders

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.MainActivity
import com.crmbl.thesafe.R
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    fun bind(file : File) {
        val splitedName = file.originName.split('.')
        itemView.findViewById<TextView>(R.id.textview_title).text = splitedName.first()
        itemView.findViewById<TextView>(R.id.textview_ext).text = splitedName.last()

        when {
            splitedName.last().toLowerCase() == "gif" -> {
                itemView.findViewById<GifImageView>(R.id.imageView).setImageDrawable(
                    GifDrawable(file.decrypted!!.inputStream()))
            }
            else -> {
                itemView.findViewById<GifImageView>(R.id.imageView).setImageDrawable(
                    BitmapDrawable(Resources.getSystem(), file.decrypted!!.inputStream()))
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

    fun recycleView(activity: MainActivity) {
        val mediaView = itemView.findViewById<GifImageView>(R.id.imageView)
        if (mediaView.drawable == null) return

        val mediaViewDrawable = mediaView.drawable
        if (mediaViewDrawable is GifDrawable)
            mediaViewDrawable.recycle()
        if (mediaViewDrawable is BitmapDrawable)
            mediaViewDrawable.bitmap.recycle()

        activity.runOnUiThread { mediaView.setImageDrawable(null) }
    }
}