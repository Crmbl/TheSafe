package com.crmbl.thesafe.viewHolders

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.File
import com.crmbl.thesafe.R
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val textViewTitle : TextView = itemView.findViewById(R.id.textview_title)
    private val textViewExt : TextView = itemView.findViewById(R.id.textview_ext)
    private val mediaView : GifImageView = itemView.findViewById(R.id.imageView)

    fun bind(file : File) {
        val splitedName = file.originName.split('.')
        textViewTitle.text = splitedName.first()
        textViewExt.text = splitedName.last()

        when {
            splitedName.last().toLowerCase() == "gif" ->
                mediaView.setImageDrawable(GifDrawable(file.decrypted!!))
            else ->
                mediaView.setImageDrawable(BitmapDrawable(Resources.getSystem(), file.decrypted!!.inputStream()))
        }
    }

    fun clearAnimation() {
        itemView.clearAnimation()
    }
}