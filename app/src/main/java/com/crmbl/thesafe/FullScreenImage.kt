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
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.Target
import com.crmbl.thesafe.listeners.ComposableAnimatorListener


@SuppressLint("ClickableViewAccessibility", "InflateParams")
class FullScreenImage(mContext: Context, v: View, imageBytes: ByteArray, fileExt : String) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
            R.layout.image_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    internal var view: View = contentView
    internal var photoView: PhotoView
    internal var loading: ProgressBar
    private var lockLayout: FrameLayout
    private var rotateButton: ImageButton
    private var closeButton: ImageButton
    private var isLandscape: Boolean = false
    private var frame: RelativeLayout
    private var gif : GifDrawable? = null
    private var bitmap : Bitmap? = null
    var isScaling: Boolean = false

    init {
        isOutsideTouchable = true
        elevation = 5.0f
        isFocusable = true

        frame = view.findViewById(R.id.rl_custom_layout)
        lockLayout = view.findViewById(R.id.layout_lock)
        photoView = view.findViewById(R.id.image)
        rotateButton = view.findViewById(R.id.controller_rotate)
        loading = view.findViewById(R.id.loading)
        closeButton = this.view.findViewById(R.id.ib_close) as ImageButton

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
                            gif = resource
                            photoView.setImageDrawable(gif)
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
                            bitmap = resource
                            photoView.setImageBitmap(bitmap)
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
        lockLayout.visibility = View.VISIBLE
    }

    fun onResume() {
        lockLayout.visibility = View.GONE
    }

    private fun rotate() {
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

            //TODO when isLandscape, can't scroll vertically
            if (isLandscape)
                photoView.scale = 1f
        })
        animatorSet.start()
    }

    override fun dismiss() {
        photoView.setImageDrawable(null)
        if (bitmap != null) bitmap?.recycle(); bitmap = null
        if (gif != null) gif?.recycle(); gif = null

        super.dismiss()
    }
}