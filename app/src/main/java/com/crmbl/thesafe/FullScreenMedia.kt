package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.view.Gravity
import android.graphics.Bitmap
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import android.content.Context
import com.github.chrisbanes.photoview.PhotoView
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.LayoutInflater
import android.view.View
import android.widget.*
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
    internal var videoView: VideoView
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
        videoView = view.findViewById(R.id.video)
        loading = view.findViewById(R.id.loading)
        photoView.maximumScale = 6f
        parent = photoView.parent as ViewGroup
        loading.isIndeterminate = true
        loading.visibility = View.VISIBLE

        val imageFileExtensions: Array<String> = arrayOf("png", "jpg", "jpeg", "bmp", "pdf")
        if (fileExt.toLowerCase() == "gif") {
            photoView.visibility = View.VISIBLE
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
        } else if (imageFileExtensions.contains(fileExt.toLowerCase())) {
            photoView.visibility = View.VISIBLE
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
        else {
            videoView.visibility = View.VISIBLE
            //TODO videoView mediaController...
        }

        showAtLocation(v, Gravity.CENTER, 0, 0)
    }
}