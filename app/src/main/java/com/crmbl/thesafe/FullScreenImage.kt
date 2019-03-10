package com.crmbl.thesafe

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.view.*
import android.widget.*
import com.crmbl.thesafe.utils.CryptoUtil
import kotlinx.android.synthetic.main.image_fullscreen.view.*
import kotlinx.coroutines.*
import pl.droidsonroids.gif.GifDrawable
import java.io.ByteArrayInputStream
import com.crmbl.thesafe.listeners.*


@SuppressLint("ClickableViewAccessibility", "InflateParams")
class FullScreenImage(mContext: Context, v: View, file: File) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
            R.layout.image_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    internal var view: View = contentView
    private var isLandscape: Boolean = false
    private var isRotating: Boolean = false

    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var limitFactor: Float = 1f
    private var scaleFactor: Float = 1f
    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f
    private var mActivePointerId: Int = 0
    private var imageX: Float? = null
    private var imageY: Float? = null
    var isScaling: Boolean = false

    init {
        isOutsideTouchable = true
        elevation = 5.0f
        isFocusable = true

        view.loading.isIndeterminate = true
        view.loading.visibility = View.VISIBLE

        //region init listeners

        view.ib_close.setOnClickListener { dismiss() }
        view.controller_rotate.setOnClickListener { rotateImage() }
        gestureDetector = GestureDetector(mContext, ComposableGestureListener().onDoubleTap { fastZoom(); true })
        scaleGestureDetector = ScaleGestureDetector(mContext, ComposableScaleGestureListener().onScale { detector -> scaleImage(detector) })

        view.rl_custom_layout.setOnTouchListener(ComposableTouchListener { _, event -> onTouchFrame(event!!) })
        view.image.setOnTouchListener(ComposableTouchListener { _, event -> onTouchImage(event!!) })
        view.image.viewTreeObserver.addOnDrawListener { onDraw() }

        //endregion init listeners

        var decryptedStream : ByteArrayInputStream? = null
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val deferred = async(Dispatchers.Default) {
                decryptedStream = CryptoUtil.decrypt(java.io.File(file.path))!!.inputStream()
            }

            deferred.await()

            val fileExt = file.originName.split('.').last()
            val imageFileExtensions: Array<String> = arrayOf("png", "jpg", "jpeg", "bmp", "pdf")
            when {
                fileExt.toLowerCase() == "gif" -> {
                    view.image.setImageDrawable(GifDrawable(decryptedStream!!))}
                imageFileExtensions.contains(fileExt.toLowerCase()) -> {
                    view.image.setImageDrawable(BitmapDrawable(Resources.getSystem(), decryptedStream!!))}
            }

            view.loading.visibility = View.GONE
            showAtLocation(v, Gravity.CENTER, 0, 0)
        }
    }

    fun onPause() {
        view.layout_lock.visibility = View.VISIBLE
    }

    fun onResume() {
        view.layout_lock.visibility = View.GONE
    }

    private fun rotateImage() {
        isRotating = true
        if (imageX == null && imageY == null) {
            imageX = view.image.x
            imageY = view.image.y
        } else {
            view.image.x = imageX!!
            view.image.y = imageY!!
        }

        val rotateDegree: Float = if (isLandscape) 0f else 90f
        val scaleFactor: Float = if (isLandscape) 1f
            else {
                if (view.image.width >= view.image.height) view.rl_custom_layout.width.toFloat() / view.image.height.toFloat()
                else view.rl_custom_layout.height.toFloat() / view.image.width.toFloat()
        }

        val animatorSet = AnimatorSet()
        animatorSet.duration = 500
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(view.image, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(view.controller_rotate, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(view.image, "scaleX", scaleFactor),
            ObjectAnimator.ofFloat(view.image, "scaleY", scaleFactor)
        )
        animatorSet.addListener(ComposableAnimatorListener {
            this.scaleFactor = scaleFactor
            limitFactor = scaleFactor
            isRotating = false
            isLandscape = !isLandscape
        })
        animatorSet.start()
    }

    private fun fastZoom() {
        when {
            scaleFactor < 2.5f -> scaleFactor = 2.5f
            scaleFactor < 5f -> scaleFactor = 5f
            scaleFactor == 5f -> {
                scaleFactor = if (isLandscape) limitFactor
                              else 1f
            }
        }

        view.image.invalidate()
    }

    private fun onDraw() {
        if (isRotating) return
        isScaling = scaleFactor != 1f
        view.image.scaleX = scaleFactor
        view.image.scaleY = scaleFactor
    }

    private fun onTouchFrame(event: MotionEvent): Boolean {
        gestureDetector!!.onTouchEvent(event)
        view.rl_custom_layout.requestDisallowInterceptTouchEvent(true)
        return true
    }

    private fun scaleImage(detector: ScaleGestureDetector): Boolean {
        scaleFactor *= detector.scaleFactor
        val tmp = Math.max(0.1f, Math.min(scaleFactor, 5.0f))
        scaleFactor = if (tmp < limitFactor) limitFactor else tmp
        view.image.invalidate()

        return false
    }

    private fun onTouchImage(event: MotionEvent): Boolean {
        scaleGestureDetector!!.onTouchEvent(event)
        gestureDetector!!.onTouchEvent(event)
        onTouchEvent(event)
        return true
    }

    private fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                event.actionIndex.also { pointerIndex ->
                    mLastTouchX = event.getX(pointerIndex)
                    mLastTouchY = event.getY(pointerIndex)
                }
                mActivePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val (x: Float, y: Float) = event.findPointerIndex(mActivePointerId).let { pointerIndex ->
                    event.getX(pointerIndex) to event.getY(pointerIndex)
                }
                if (isLandscape)
                    checkClipping(-y + mLastTouchY, x - mLastTouchX)
                else
                    checkClipping(x - mLastTouchX, y - mLastTouchY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mActivePointerId = MotionEvent.INVALID_POINTER_ID
            MotionEvent.ACTION_POINTER_UP -> {
                event.actionIndex.also { pointerIndex -> event.getPointerId(pointerIndex)
                    .takeIf {
                        it == mActivePointerId
                    }?.run {
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        mLastTouchX = event.getX(newPointerIndex)
                        mLastTouchY = event.getY(newPointerIndex)
                        mActivePointerId = event.getPointerId(newPointerIndex)
                    }
                }
            }
        }
        return true
    }

    private fun checkClipping(x: Float, y: Float) {
        if (imageX == null || imageY == null) {
            imageX = view.image.x
            imageY = view.image.y
        }

        val videoWidth: Float
        val videoHeight: Float
        if (!isLandscape) {
            videoWidth = view.image.width * scaleFactor
            videoHeight = view.image.height * scaleFactor
        } else {
            videoWidth = view.image.height * scaleFactor
            videoHeight = view.image.width * scaleFactor
        }

        val tmpX = (videoWidth - view.rl_custom_layout.width) / 2
        val tmpY = (videoHeight - view.rl_custom_layout.height) / 2
        val nextX = view.image.x + x
        val nextY = view.image.y + y

        if (videoWidth > view.rl_custom_layout.width) {
            if (nextX <= tmpX +imageX!! && nextX >= -tmpX +imageX!!)
                view.image.x = nextX
            else if (nextX <= tmpX +imageX!!)
                view.image.x = -tmpX +imageX!!
            else
                view.image.x = tmpX +imageX!!
        }
        else if (imageX != null)
            view.image.x = imageX!!

        if (videoHeight > view.rl_custom_layout.height) {
            if (nextY <= tmpY +imageY!! && nextY >= -tmpY +imageY!!)
                view.image.y = nextY
            else if (nextY <= tmpY +imageY!!)
                view.image.y = -tmpY +imageY!!
            else
                view.image.y = tmpY +imageY!!
        }
        else if (imageY != null)
            view.image.y = imageY!!

        view.image.invalidate()
    }

    override fun dismiss() {
        val drawable = view.image.drawable
        if (drawable is BitmapDrawable) drawable.bitmap.recycle()

        view.ib_close.setOnClickListener(null)
        view.controller_rotate.setOnClickListener(null)
        view.rl_custom_layout.setOnTouchListener(null)
        view.image.setOnTouchListener(null)
        view.image.viewTreeObserver.removeOnDrawListener { onDraw() }

        view.image.setImageDrawable(null)
        gestureDetector = null
        scaleGestureDetector = null
        imageX = null
        imageY = null

        super.dismiss()
    }
}