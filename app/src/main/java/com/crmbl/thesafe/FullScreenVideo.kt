package com.crmbl.thesafe

import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.*
import android.view.MotionEvent.INVALID_POINTER_ID
import android.widget.*
import com.crmbl.thesafe.utils.UriByteDataHelper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSpec
import java.io.IOException
import android.os.Handler
import com.crmbl.thesafe.listeners.*
import com.google.android.exoplayer2.Player
import android.animation.ObjectAnimator
import com.crmbl.thesafe.utils.CryptoUtil
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DataSource
import kotlinx.android.synthetic.main.video_fullscreen.view.*
import kotlinx.coroutines.*


@Suppress("DEPRECATION")
@SuppressLint("ClickableViewAccessibility", "InflateParams")
class FullScreenVideo(mContext: Context, v: View, file: File) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
        R.layout.video_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    internal var view: View = contentView
    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var timeoutHandler: Handler? = null
    private var interactionTimeoutRunnable: Runnable? = null
    private var updateHandler: Handler? = null
    private var updateProgressAction: Runnable? = null
    private var progressListener: TimeBar.OnScrubListener? = null
    private var listener: Player.EventListener? = null

    private var limitFactor: Float = 1f
    private var scaleFactor: Float = 1f
    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f
    private var mActivePointerId: Int = 0
    private var videoX: Float? = null
    private var videoY: Float? = null
    private var isLandscape: Boolean = false
    private var isRotating: Boolean = false
    var isScaling: Boolean = false

    init {

        //region init variables

        isOutsideTouchable = true
        isFocusable = true
        elevation = 5.0f

        view.loading.isIndeterminate = true
        view.loading.visibility = View.VISIBLE

        val player = ExoPlayerFactory.newSimpleInstance(mContext, DefaultTrackSelector())
        player?.volume = 1f
        player?.playWhenReady = true
        view.video.player = player
        view.video.hideController()

        timeoutHandler = Handler()
        interactionTimeoutRunnable = Runnable { if (player?.playWhenReady!!) view.controller_layout.visibility = View.INVISIBLE }
        resetHandler(true)
        updateHandler = Handler()
        updateProgressAction = Runnable { updateProgressBar() }

        //endregion init variables

        //region init listeners

        gestureDetector = GestureDetector(mContext, ComposableGestureListener().onDoubleTap { toggleController() })
        scaleGestureDetector = ScaleGestureDetector(mContext, ComposableScaleGestureListener().onScale { detector -> scaleVideo(detector) })
        progressListener = ComposableTimeBarScrubListener(onStop = { _, position, _ -> seekVideo(position) })
        listener = ComposablePlayerEventListener().onPlayerStateChanged { _, _ -> updateProgressBar() }

        player?.addListener(listener)
        player?.addVideoListener(ComposableVideoListener().onRenderedFirstFrame { onFirstFrame() })
        view.controller_close.setOnClickListener { dismiss() }
        view.controller_minimize.setOnClickListener { dismiss() }
        view.controller_mute.setOnClickListener { muteVideo() }
        view.controller_volume.setOnClickListener { unMuteVideo() }
        view.controller_pause.setOnClickListener { pauseVideo() }
        view.controller_play.setOnClickListener { playVideo() }
        view.controller_rotate.setOnClickListener { rotateVideo() }
        view.controller_progress.addListener(progressListener)
        view.rl_custom_layout.setOnTouchListener(ComposableTouchListener { _, event -> onTouchFrame(event!!) })
        view.video.setOnTouchListener(ComposableTouchListener { _, event -> onTouchVideo(event!!) })
        view.video.viewTreeObserver.addOnDrawListener { onDraw() }

        //endregion init listeners

        //region init video

        var decrypted : ByteArray? = null
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val deferred = async(Dispatchers.Default) {
                decrypted = CryptoUtil.decrypt(java.io.File(file.path))
            }

            deferred.await()
            val byteArrayDataSource = ByteArrayDataSource(decrypted)
            val mediaByteUri = UriByteDataHelper().getUri(decrypted!!)
            val dataSpec = DataSpec(mediaByteUri)
            try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
            val factory = object : DataSource.Factory { override fun createDataSource(): DataSource { return byteArrayDataSource } }
            val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)
            (view.video.player as SimpleExoPlayer).prepare(mediaSource)
            showAtLocation(v, Gravity.CENTER, 0, 0)
        }

        //endregion init video
    }

    //region private methods

    private fun showController() {
        view.controller_layout.clearAnimation()
        view.controller_layout.visibility = View.VISIBLE
    }

    private fun hideController() {
        if (view.controller_layout.visibility == View.VISIBLE)
            view.controller_layout.visibility = View.INVISIBLE
    }

    private fun pauseVideo() {
        view.controller_pause.visibility = View.GONE
        view.controller_play.visibility = View.VISIBLE
        view.video.player?.playWhenReady = false
        updateHandler?.removeCallbacks(updateProgressAction)
    }

    private fun playVideo() {
        view.controller_play.visibility = View.GONE
        view.controller_pause.visibility = View.VISIBLE
        view.video.player?.playWhenReady = true
        hideController()
    }

    private fun resetHandler(start: Boolean = false) {
        if (!start)
            timeoutHandler!!.removeCallbacks(interactionTimeoutRunnable)
        timeoutHandler!!.postDelayed(interactionTimeoutRunnable, 5000)
    }

    private fun muteVideo() {
        resetHandler()
        (view.video.player as SimpleExoPlayer).volume = 1f
        view.controller_mute.visibility = View.GONE
        view.controller_volume.visibility = View.VISIBLE
    }

    private fun unMuteVideo() {
        resetHandler()
        (view.video.player as SimpleExoPlayer).volume = 0f
        view.controller_mute.visibility = View.VISIBLE
        view.controller_volume.visibility = View.GONE
    }

    private fun updateProgressBar() {
        val duration = (if (view.video.player == null) 0 else view.video.player!!.duration).toLong()
        val position = (if (view.video.player == null) 0 else view.video.player?.currentPosition)!!.toLong()
        val bufferedPosition = (if (view.video.player == null) 0 else view.video.player?.bufferedPosition)!!.toLong()

        view.controller_progress.setPosition(position)
        view.controller_progress.setBufferedPosition(bufferedPosition)
        view.controller_progress.setDuration(duration)

        updateHandler?.removeCallbacks(updateProgressAction)
        val playbackState = if (view.video.player == null) Player.STATE_IDLE else view.video.player?.playbackState
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            var delayMs: Long
            if (view.video.player?.playWhenReady!! && playbackState == Player.STATE_READY) {
                delayMs = 800 - position % 800
                if (delayMs < 200) delayMs += 800
            } else delayMs = 800
            updateHandler?.postDelayed(updateProgressAction, delayMs)
        }
    }

    private fun rotateVideo() {
        resetHandler()
        isRotating = true
        view.video.x = videoX!!
        view.video.y = videoY!!

        val rotateDegree: Float = if (isLandscape) 0f else 90f
        val scaleFactor: Float = if (isLandscape) 1f
            else {
                if (view.video.width >= view.video.height) view.rl_custom_layout.width.toFloat() / view.video.height.toFloat()
                else view.rl_custom_layout.height.toFloat() / view.video.width.toFloat()
            }

        val animatorSet = AnimatorSet()
        animatorSet.duration = 500
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(view.video, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(view.controller_pause, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(view.controller_play, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(view.controller_mute, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(view.controller_volume, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(view.controller_rotate, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(view.video, "scaleX", scaleFactor),
            ObjectAnimator.ofFloat(view.video, "scaleY", scaleFactor)
        )
        animatorSet.addListener(ComposableAnimatorListener {
            this.scaleFactor = scaleFactor
            limitFactor = scaleFactor
            isRotating = false
            isLandscape = !isLandscape
        })
        animatorSet.start()
    }

    private fun seekVideo(position: Long) {
        resetHandler()
        view.video.player?.seekTo(position)
    }

    private fun toggleController(): Boolean {
        if (view.controller_layout.visibility == View.VISIBLE)
            hideController()
        else
            showController()
        return true
    }

    private fun onDraw() {
        if (isRotating) return
        isScaling = scaleFactor != 1f
        view.video.scaleX = scaleFactor
        view.video.scaleY = scaleFactor
    }

    private fun onTouchFrame(event: MotionEvent): Boolean {
        resetHandler()
        gestureDetector!!.onTouchEvent(event)
        view.rl_custom_layout.requestDisallowInterceptTouchEvent(true)
        return true
    }

    private fun scaleVideo(detector: ScaleGestureDetector): Boolean {
        scaleFactor *= detector.scaleFactor
        val tmp = Math.max(0.1f, Math.min(scaleFactor, 5.0f))
        scaleFactor = if (tmp < limitFactor) limitFactor else tmp
        view.video.invalidate()

        return false
    }

    private fun onFirstFrame() {
        view.loading.visibility = View.GONE
    }

    private fun onTouchVideo(event: MotionEvent): Boolean {
        resetHandler()
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

    private fun checkClipping(x: Float, y: Float) {
        if (videoX == null || videoY == null) {
            videoX = view.video.x
            videoY = view.video.y
        }

        val videoWidth: Float
        val videoHeight: Float
        if (!isLandscape) {
            videoWidth = view.video.width * scaleFactor
            videoHeight = view.video.height * scaleFactor
        } else {
            videoWidth = view.video.height * scaleFactor
            videoHeight = view.video.width * scaleFactor
        }

        val tmpX = (videoWidth - view.rl_custom_layout.width) / 2
        val tmpY = (videoHeight - view.rl_custom_layout.height) / 2
        val nextX = view.video.x + x
        val nextY = view.video.y + y

        if (videoWidth > view.rl_custom_layout.width) {
            if (nextX <= tmpX +videoX!! && nextX >= -tmpX +videoX!!)
                view.video.x = nextX
            else if (nextX <= tmpX +videoX!!)
                view.video.x = -tmpX +videoX!!
            else
                view.video.x = tmpX +videoX!!
        }
        else if (videoX != null)
            view.video.x = videoX!!

        if (videoHeight > view.rl_custom_layout.height) {
            if (nextY <= tmpY +videoY!! && nextY >= -tmpY +videoY!!)
                view.video.y = nextY
            else if (nextY <= tmpY +videoY!!)
                view.video.y = -tmpY +videoY!!
            else
                view.video.y = tmpY +videoY!!
        }
        else if (videoY != null)
            view.video.y = videoY!!

        view.video.invalidate()
    }

    //endregion private methods

    //region public methods

    fun onPause() {
        pauseVideo()
        updateHandler?.removeCallbacks(updateProgressAction)
        timeoutHandler?.removeCallbacks(interactionTimeoutRunnable)
        view.layout_lock.visibility = View.VISIBLE
    }

    fun onResume() {
        view.layout_lock.visibility = View.GONE
        showController()
    }

    //endregion public methods

    //region override

    override fun dismiss() {
        view.video.player?.release()
        updateHandler?.removeCallbacks(updateProgressAction)
        timeoutHandler?.removeCallbacks(interactionTimeoutRunnable)
        view.controller_close.setOnClickListener(null)
        view.controller_minimize.setOnClickListener(null)
        view.controller_mute.setOnClickListener(null)
        view.controller_volume.setOnClickListener(null)
        view.controller_pause.setOnClickListener(null)
        view.controller_play.setOnClickListener(null)
        view.controller_rotate.setOnClickListener(null)
        view.video.setOnTouchListener(null)
        view.rl_custom_layout.setOnTouchListener(null)

        (view.video.player as SimpleExoPlayer).removeListener(listener)
        (view.video.player as SimpleExoPlayer).setVideoListener(null)
        view.controller_progress.removeListener(progressListener)
        view.video.viewTreeObserver.removeOnDrawListener { onDraw() }

        gestureDetector = null
        listener = null
        progressListener = null
        scaleGestureDetector = null
        interactionTimeoutRunnable = null
        updateProgressAction = null
        videoX = null
        videoY = null

        super.dismiss()
    }

    //endregion override
}