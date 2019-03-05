package com.crmbl.thesafe

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import android.content.Context
import com.github.chrisbanes.photoview.PhotoView
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.graphics.drawable.BitmapDrawable
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.Target
import com.crmbl.thesafe.listeners.ComposableAnimatorListener


//TODO when isLandscape, can't scroll vertically
@SuppressLint("ClickableViewAccessibility", "InflateParams")
class FullScreenImage(mContext: Context, v: View, imageBytes: ByteArray, fileExt : String) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
            R.layout.image_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    private var isLandscape: Boolean = false
    var isScaling: Boolean = false

    init {
        isOutsideTouchable = true
        elevation = 5.0f
        isFocusable = true

        val photoView = contentView.findViewById<PhotoView>(R.id.image)
        val loading = contentView.findViewById<ProgressBar>(R.id.loading)
        val rotateButton = contentView.findViewById<ImageButton>(R.id.controller_rotate)
        val closeButton = contentView.findViewById<ImageButton>(R.id.ib_close)

        closeButton.setOnClickListener { dismiss() }
        rotateButton.setOnClickListener { rotate() }

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

        photoView.setOnScaleChangeListener { _, _, _ -> this@FullScreenImage.isScaling = photoView.scale > 1f  }
        showAtLocation(v, Gravity.CENTER, 0, 0)
    }

    fun onPause() {
        contentView.findViewById<FrameLayout>(R.id.layout_lock).visibility = View.VISIBLE
    }

    fun onResume() {
        contentView.findViewById<FrameLayout>(R.id.layout_lock).visibility = View.GONE
    }

    private fun rotate() {
        val photoView = contentView.findViewById<PhotoView>(R.id.image)
        val frame = contentView.findViewById<RelativeLayout>(R.id.rl_custom_layout)
        val rotateButton = contentView.findViewById<ImageButton>(R.id.controller_rotate)

        photoView.isZoomable = false
        val rotateDegree: Float = if (isLandscape) 0f else 90f
        val scaleFactor: Float = if (isLandscape) 1f
            else {
                if (photoView.width >= photoView.height) frame.width.toFloat() / photoView.height.toFloat()
                else frame.height.toFloat() / photoView.width.toFloat()
        }

        val animatorSet = AnimatorSet()
        animatorSet.duration = 500
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(photoView, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(rotateButton, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(photoView, "scaleX", scaleFactor),
            ObjectAnimator.ofFloat(photoView, "scaleY", scaleFactor)
        )
        animatorSet.addListener(ComposableAnimatorListener {
            isLandscape = !isLandscape
            photoView.isZoomable = true
            photoView.scale = 1f
        })
        animatorSet.start()
    }

    override fun dismiss() {
        val drawable = contentView.findViewById<PhotoView>(R.id.image).drawable
        if (drawable is BitmapDrawable) drawable.bitmap.recycle()
        //if (drawable is GifDrawable) drawable.recycle()

        contentView.findViewById<PhotoView>(R.id.image).setImageDrawable(null)
        super.dismiss()
    }
}