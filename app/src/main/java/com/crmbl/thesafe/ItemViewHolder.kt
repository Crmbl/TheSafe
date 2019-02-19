package com.crmbl.thesafe

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView


class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewTitle : TextView = itemView.findViewById(R.id.textview_title)
    private val textViewExt : TextView = itemView.findViewById(R.id.textview_ext)
    private val mediaView : GifImageView = itemView.findViewById(R.id.imageView) as GifImageView

    fun bind(file : File) {
        val splitedName = file.originName.split('.')
        textViewTitle.text = splitedName.first()
        textViewExt.text = splitedName.last()
        mediaView.setImageDrawable(GifDrawable(file.decrypted!!))
    }
}