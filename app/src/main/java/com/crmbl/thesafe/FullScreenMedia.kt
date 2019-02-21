package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.view.Gravity
import android.graphics.Bitmap
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import android.content.Context
import android.widget.ProgressBar
import com.github.chrisbanes.photoview.PhotoView
import android.widget.ImageButton
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupWindow
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.Target


@SuppressLint("InflateParams")
class FullScreenMedia(internal var mContext: Context, v: View, imageBytes: ByteArray, fileExt : String) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
            R.layout.media_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    internal var view: View
    internal var photoView: PhotoView
    internal var loading: ProgressBar
    internal var lockLayout: FrameLayout
    private var parent: ViewGroup

    init {
        elevation = 5.0f
        this.view = contentView
        val closeButton = this.view.findViewById(R.id.ib_close) as ImageButton
        isOutsideTouchable = true

        isFocusable = true
        closeButton.setOnClickListener { dismiss() }

        lockLayout = view.findViewById(R.id.layout_lock)
        photoView = view.findViewById(R.id.image)
        loading = view.findViewById(R.id.loading)
        photoView.maximumScale = 6f
        parent = photoView.parent as ViewGroup
        loading.isIndeterminate = true
        loading.visibility = View.VISIBLE

        if (fileExt.toLowerCase() == "gif") {
            Glide.with(mContext).asGif()
                .load(imageBytes)
                .listener(object : RequestListener<GifDrawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean {
                        loading.isIndeterminate = false
                        loading.setBackgroundColor(mContext.getColor(R.color.colorItemBackground))
                        return false
                    }
                    override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        loading.setBackgroundColor(mContext.getColor(R.color.colorItemBackground))
                        photoView.setImageDrawable(resource)
                        loading.visibility = View.GONE
                        return false
                    }
                }).into(photoView)
        } /*else if () {
            //TODO video
        }*/
        else {
            Glide.with(mContext).asBitmap()
                .load(imageBytes)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                        loading.isIndeterminate = false
                        loading.setBackgroundColor(mContext.getColor(R.color.colorItemBackground))
                        return false
                    }
                    override fun onResourceReady(resource: Bitmap, model: Any, target: com.bumptech.glide.request.target.Target<Bitmap>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        loading.setBackgroundColor(mContext.getColor(R.color.colorItemBackground))
                        photoView.setImageBitmap(resource)
                        loading.visibility = View.GONE
                        return false
                    }
                }).into(photoView)
        }

        showAtLocation(v, Gravity.CENTER, 0, 0)
    }
}