package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.graphics.Rect
import android.graphics.RectF
import android.view.*
import android.view.MotionEvent.INVALID_POINTER_ID
import android.widget.*
import com.crmbl.thesafe.utils.UriByteDataHelper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.exo_controller_fullscreen.view.*
import java.io.IOException
import com.google.android.exoplayer2.ui.PlayerView


@SuppressLint("ClickableViewAccessibility", "InflateParams")
class FullScreenVideo(internal var mContext: Context, v: View, imageBytes: ByteArray) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
        R.layout.video_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    internal var view: View = contentView
    internal var loading: ProgressBar
    internal var videoView: PlayerView
    private var lockLayout: FrameLayout
    private var player: SimpleExoPlayer? = null
    //////////////////////////TODO TESTING///////////////////////
    private var scaleFactor: Float = 1f
    private val mContentRect: Rect = Rect()
    private var mScaleGestureDetector : ScaleGestureDetector
    private val AXIS_X_MIN = -1f
    private val AXIS_X_MAX = 1f
    private val AXIS_Y_MIN = -1f
    private val AXIS_Y_MAX = 1f
    private val mCurrentViewport = RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX)
    private var mLastTouchX : Float = 0f
    private var mLastTouchY : Float = 0f
    private var mActivePointerId : Int = 0
    //////////////////////////TODO TESTING///////////////////////

    init {
        isOutsideTouchable = true
        isFocusable = true
        elevation = 5.0f

        val closeButton = this.view.findViewById(R.id.ib_close) as ImageButton
        lockLayout = view.findViewById(R.id.layout_lock)
        videoView = view.findViewById(R.id.video)
        loading = view.findViewById(R.id.loading)
        loading.isIndeterminate = true
        loading.visibility = View.VISIBLE

        player = ExoPlayerFactory.newSimpleInstance(mContext, DefaultTrackSelector())
        player?.volume = 1f
        player?.playWhenReady = true
        videoView.player = player

        //region listeners

        closeButton.setOnClickListener { dismiss() }

        player?.addVideoListener(object: VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                mContentRect.set(videoView.paddingLeft, videoView.paddingTop,
                    videoView.width - videoView.paddingRight, videoView.height - videoView.paddingBottom)
            }
            override fun onRenderedFirstFrame() {
                loading.setBackgroundColor(mContext.getColor(R.color.colorItemBackground))
                loading.visibility = View.GONE
                videoView.showController()
            }
        })

        /*videoView.exo_quit_fullscreen.setOnClickListener { this.dismiss() }
        videoView.exo_mute.setOnClickListener {
            player?.volume = 1f
            videoView.exo_mute.visibility = View.GONE
            videoView.exo_volume.visibility = View.VISIBLE
        }
        videoView.exo_volume.setOnClickListener {
            player?.volume = 0f
            videoView.exo_mute.visibility = View.VISIBLE
            videoView.exo_volume.visibility = View.GONE
        }*/

        //////////////////////////TODO TESTING///////////////////////
        mScaleGestureDetector = ScaleGestureDetector(mContext, object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScaleEnd(detector: ScaleGestureDetector) {}
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean { return true }
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f))
                videoView.invalidate()
                return false
            }
        })
        val mGestureDetector = GestureDetector(mContext, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                mContentRect.apply {
                    val viewportOffsetX = distanceX * mCurrentViewport.width() / width()
                    val viewportOffsetY = -distanceY * mCurrentViewport.height() / height()

                    val curWidth: Float = mCurrentViewport.width()
                    val curHeight: Float = mCurrentViewport.height()
                    val newX: Float = Math.max(AXIS_X_MIN, Math.min(mCurrentViewport.left + viewportOffsetX, AXIS_X_MAX - curWidth))
                    val newY: Float = Math.max(AXIS_Y_MIN + curHeight, Math.min(mCurrentViewport.bottom + viewportOffsetY, AXIS_Y_MAX))
                    mCurrentViewport.set(newX, newY - curHeight, newX + curWidth, newY)

                    videoView.postInvalidateOnAnimation()
                }
                return true
            }
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }
        })
        /*mScaleGestureDetector = ScaleGestureDetector(mContext, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private val viewportFocus = PointF()
            private var lastSpanX: Float = 0f
            private var lastSpanY: Float = 0f

            override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
                lastSpanX = scaleGestureDetector.currentSpanX
                lastSpanY = scaleGestureDetector.currentSpanY
                return true
            }

            override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
                val spanX: Float = scaleGestureDetector.currentSpanX
                val spanY: Float = scaleGestureDetector.currentSpanY

                val newWidth: Float = lastSpanX / spanX * mCurrentViewport.width()
                val newHeight: Float = lastSpanY / spanY * mCurrentViewport.height()

                val focusX: Float = scaleGestureDetector.focusX
                val focusY: Float = scaleGestureDetector.focusY
                if (mContentRect.contains(focusX.toInt(), focusY.toInt())) {
                    viewportFocus.set(
                        mCurrentViewport.left + mCurrentViewport.width() * (x - mContentRect.left) / mContentRect.width(),
                        mCurrentViewport.top + mCurrentViewport.height() * (y - mContentRect.bottom) / -mContentRect.height()
                    )
                }

                mContentRect?.apply {
                    mCurrentViewport.set(
                        viewportFocus.x - newWidth * (focusX - left) / mCurrentViewport.width(),
                        viewportFocus.y - newHeight * (bottom - focusY) / mCurrentViewport.height(),
                        0f,
                        0f
                    )
                }
                mCurrentViewport.right = mCurrentViewport.left + newWidth
                mCurrentViewport.bottom = mCurrentViewport.top + newHeight
                videoView.postInvalidateOnAnimation()

                lastSpanX = spanX
                lastSpanY = spanY
                return true
            }
        })*/
        videoView.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                //return mScaleGestureDetector.onTouchEvent(event)
                //return mGestureDetector.onTouchEvent(event)
                return onTouchEvent(event)
            }
        })

        videoView.viewTreeObserver.addOnDrawListener {
            videoView.scaleX = scaleFactor
            videoView.scaleY = scaleFactor
        }
        //////////////////////////TODO TESTING///////////////////////

        //endregion

        //region init video

        val byteArrayDataSource = ByteArrayDataSource(imageBytes)
        val mediaByteUri = UriByteDataHelper().getUri(imageBytes)
        val dataSpec = DataSpec(mediaByteUri)

        try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
        val factory = object : com.google.android.exoplayer2.upstream.DataSource.Factory {
            override fun createDataSource(): com.google.android.exoplayer2.upstream.DataSource { return byteArrayDataSource } }

        val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)
        player?.prepare(mediaSource)

        showAtLocation(v, Gravity.CENTER, 0, 0)

        //endregion init video
    }

    //////////////////////////TODO TESTING///////////////////////
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
                videoView.x += x - mLastTouchX
                videoView.y += y - mLastTouchY
                videoView.invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mActivePointerId = INVALID_POINTER_ID
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
    //////////////////////////TODO TESTING///////////////////////

    override fun dismiss() {
        player?.release()
        super.dismiss()
    }

    fun onPause() {
        videoView.exo_pause.performClick()
        lockLayout.visibility = View.VISIBLE
    }

    fun onResume() {
        videoView.exo_play.performClick()
        lockLayout.visibility = View.GONE
    }
}