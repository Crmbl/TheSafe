package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import android.content.Context
import com.github.chrisbanes.photoview.PhotoView
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.Target


@SuppressLint("ClickableViewAccessibility", "InflateParams")
class FullScreenImage(mContext: Context, v: View, imageBytes: ByteArray, fileExt : String) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
            R.layout.image_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    internal var view: View = contentView
    internal var photoView: PhotoView
    internal var loading: ProgressBar
    private var lockLayout: FrameLayout

    init {
        isOutsideTouchable = true
        elevation = 5.0f
        isFocusable = true

        lockLayout = view.findViewById(R.id.layout_lock)
        photoView = view.findViewById(R.id.image)
        loading = view.findViewById(R.id.loading)
        val closeButton = this.view.findViewById(R.id.ib_close) as ImageButton
        closeButton.setOnClickListener { dismiss() }

        photoView.maximumScale = 6f
        loading.isIndeterminate = true
        loading.visibility = View.VISIBLE

        val imageFileExtensions: Array<String> = arrayOf("png", "jpg", "jpeg", "bmp", "pdf")
        when {
            fileExt.toLowerCase() == "gif" -> {
                Glide.with(mContext).asGif()
                    .load(imageBytes)
                    .listener(object : RequestListener<GifDrawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean { return false }
                        override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            photoView.setImageDrawable(resource)
                            loading.visibility = View.GONE
                            return false
                        }
                    }).into(photoView)
            }
            imageFileExtensions.contains(fileExt.toLowerCase()) -> {
                Glide.with(mContext).asBitmap()
                    .load(imageBytes)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Bitmap>?, isFirstResource: Boolean): Boolean { return false }
                        override fun onResourceReady(resource: Bitmap, model: Any, target: com.bumptech.glide.request.target.Target<Bitmap>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            photoView.setImageBitmap(resource)
                            loading.visibility = View.GONE
                            return false
                        }
                    }).into(photoView)
            }
        }

        showAtLocation(v, Gravity.CENTER, 0, 0)
    }

    fun onPause() {
        lockLayout.visibility = View.VISIBLE
    }

    fun onResume() {
        lockLayout.visibility = View.GONE
    }
}