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
import com.google.android.exoplayer2.ui.PlayerView
import android.os.Handler
import com.crmbl.thesafe.listeners.*
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.DefaultTimeBar
import android.animation.ObjectAnimator


@SuppressLint("ClickableViewAccessibility", "InflateParams")
class FullScreenVideo(mContext: Context, v: View, imageBytes: ByteArray) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
        R.layout.video_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    internal var view: View = contentView
    private var frame: RelativeLayout
    private var lockLayout: FrameLayout
    private var progressBar: DefaultTimeBar
    private var controllerLayout: RelativeLayout
    private var closeButton: ImageButton
    private var pauseButton: ImageButton
    private var playButton: ImageButton
    private var minimizeButton: ImageButton
    private var muteButton: ImageButton
    private var rotateButton: ImageButton
    private var volumeButton: ImageButton
    private var loading: ProgressBar
    private var frameGestureDetector: GestureDetector
    private var gestureDetector: GestureDetector
    private var scaleGestureDetector: ScaleGestureDetector
    private var player: SimpleExoPlayer? = null
    private var timeoutHandler: Handler? = null
    private var interactionTimeoutRunnable: Runnable? = null
    private var updateHandler: Handler? = null
    private var updateProgressAction: Runnable? = null
    private var limitFactor: Float = 1f
    private var isRotating: Boolean = false
    private var scaleFactor: Float = 1f
    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f
    private var mActivePointerId: Int = 0
    private var videoView: PlayerView
    private var videoX: Float? = null
    private var videoY: Float? = null
    private var isLandscape: Boolean = false
    var isScaling: Boolean = false

    init {

        //region init variables

        isOutsideTouchable = true
        isFocusable = true
        elevation = 5.0f

        frame = view.findViewById(R.id.rl_custom_layout)
        closeButton = this.view.findViewById(R.id.controller_close)
        rotateButton = this.view.findViewById(R.id.controller_rotate)
        pauseButton = this.view.findViewById(R.id.controller_pause)
        playButton = this.view.findViewById(R.id.controller_play)
        minimizeButton = this.view.findViewById(R.id.controller_minimize)
        muteButton = this.view.findViewById(R.id.controller_mute)
        volumeButton = this.view.findViewById(R.id.controller_volume)
        lockLayout = this.view.findViewById(R.id.layout_lock)
        controllerLayout = this.view.findViewById(R.id.controller_layout)
        progressBar = this.view.findViewById(R.id.controller_progress)
        videoView = this.view.findViewById(R.id.video)
        loading = this.view.findViewById(R.id.loading)
        loading.isIndeterminate = true
        loading.visibility = View.VISIBLE

        player = ExoPlayerFactory.newSimpleInstance(mContext, DefaultTrackSelector())
        player?.volume = 1f
        player?.playWhenReady = true
        videoView.player = player
        videoView.hideController()

        timeoutHandler = Handler()
        interactionTimeoutRunnable = Runnable { if (player?.playWhenReady!!) controllerLayout.visibility = View.INVISIBLE }
        resetHandler(true)
        updateHandler = Handler()
        updateProgressAction = Runnable { updateProgressBar() }

        //endregion init variables

        //region init listeners

        frameGestureDetector = GestureDetector(mContext, ComposableGestureListener().onDoubleTap { toggleController() })
        gestureDetector = GestureDetector(mContext, ComposableGestureListener().onDoubleTap { toggleController() })
        scaleGestureDetector = ScaleGestureDetector(mContext, ComposableScaleGestureListener().onScale { detector -> scaleVideo(detector) })

        player?.addListener(ComposablePlayerEventListener().onPlayerStateChanged { _, _ -> updateProgressBar() })
        player?.addVideoListener(ComposableVideoListener().onRenderedFirstFrame { onFirstFrame() })
        frame.setOnTouchListener(ComposableTouchListener { _, event -> onTouchFrame(event!!) })
        closeButton.setOnClickListener { dismiss() }
        minimizeButton.setOnClickListener { dismiss() }
        muteButton.setOnClickListener { muteVideo() }
        volumeButton.setOnClickListener { unMuteVideo() }
        pauseButton.setOnClickListener { pauseVideo() }
        playButton.setOnClickListener { playVideo() }
        rotateButton.setOnClickListener { rotateVideo() }
        progressBar.addListener(ComposableTimeBarScrubListener(onStop = { _, position, _ -> seekVideo(position) }))
        videoView.viewTreeObserver.addOnDrawListener { onDraw() }
        videoView.setOnTouchListener(ComposableTouchListener { _, event -> onTouchVideo(event!!) })

        //endregion init listeners

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

    //region private methods

    private fun showController() {
        controllerLayout.clearAnimation()
        controllerLayout.visibility = View.VISIBLE
    }

    private fun hideController() {
        if (controllerLayout.visibility == View.VISIBLE)
            controllerLayout.visibility = View.INVISIBLE
    }

    private fun pauseVideo() {
        pauseButton.visibility = View.GONE
        playButton.visibility = View.VISIBLE
        player?.playWhenReady = false
        updateHandler?.removeCallbacks(updateProgressAction)
    }

    private fun playVideo() {
        playButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE
        player?.playWhenReady = true
        hideController()
    }

    private fun resetHandler(start: Boolean = false) {
        if (!start)
            timeoutHandler!!.removeCallbacks(interactionTimeoutRunnable)
        timeoutHandler!!.postDelayed(interactionTimeoutRunnable, 5000)
    }

    private fun muteVideo() {
        resetHandler()
        player?.volume = 1f
        muteButton.visibility = View.GONE
        volumeButton.visibility = View.VISIBLE
    }

    private fun unMuteVideo() {
        resetHandler()
        player?.volume = 0f
        muteButton.visibility = View.VISIBLE
        volumeButton.visibility = View.GONE
    }

    private fun updateProgressBar() {
        val duration = (if (player == null) 0 else player!!.duration).toLong()
        val position = (if (player == null) 0 else player?.currentPosition)!!.toLong()
        val bufferedPosition = (if (player == null) 0 else player?.bufferedPosition)!!.toLong()

        progressBar.setPosition(position)
        progressBar.setBufferedPosition(bufferedPosition)
        progressBar.setDuration(duration)

        updateHandler?.removeCallbacks(updateProgressAction)
        val playbackState = if (player == null) Player.STATE_IDLE else player?.playbackState
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            var delayMs: Long
            if (player?.playWhenReady!! && playbackState == Player.STATE_READY) {
                delayMs = 800 - position % 800
                if (delayMs < 200) delayMs += 800
            } else delayMs = 800
            updateHandler?.postDelayed(updateProgressAction, delayMs)
        }
    }

    private fun rotateVideo() {
        resetHandler()
        isRotating = true
        videoView.x = videoX!!
        videoView.y = videoY!!

        val rotateDegree: Float = if (isLandscape) 0f else 90f
        val scaleFactor: Float = if (isLandscape) 1f
            else {
                if (videoView.width >= videoView.height) frame.width.toFloat() / videoView.height.toFloat()
                else frame.height.toFloat() / videoView.width.toFloat()
            }

        val animatorSet = AnimatorSet()
        animatorSet.duration = 500
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(videoView, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(pauseButton, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(playButton, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(muteButton, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(volumeButton, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(rotateButton, "rotation", rotateDegree),
            ObjectAnimator.ofFloat(videoView, "scaleX", scaleFactor),
            ObjectAnimator.ofFloat(videoView, "scaleY", scaleFactor)
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
        player?.seekTo(position)
    }

    private fun toggleController(): Boolean {
        if (controllerLayout.visibility == View.VISIBLE)
            hideController()
        else
            showController()
        return true
    }

    private fun onDraw() {
        if (isRotating) return
        isScaling = scaleFactor != 1f
        videoView.scaleX = scaleFactor
        videoView.scaleY = scaleFactor
    }

    private fun onTouchFrame(event: MotionEvent): Boolean {
        resetHandler()
        gestureDetector.onTouchEvent(event)
        frame.requestDisallowInterceptTouchEvent(true)
        return true
    }

    private fun scaleVideo(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            val tmp = Math.max(0.1f, Math.min(scaleFactor, 5.0f))
            scaleFactor = if (tmp < limitFactor) limitFactor else tmp
            videoView.invalidate()

        return false
    }

    private fun onFirstFrame() {
        loading.visibility = View.GONE
    }

    private fun onTouchVideo(event: MotionEvent): Boolean {
        resetHandler()
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
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
            videoX = videoView.x
            videoY = videoView.y
        }

        val videoWidth: Float
        val videoHeight: Float
        if (!isLandscape) {
            videoWidth = videoView.width * scaleFactor
            videoHeight = videoView.height * scaleFactor
        } else {
            videoWidth = videoView.height * scaleFactor
            videoHeight = videoView.width * scaleFactor
        }

        val tmpX = (videoWidth - frame.width) / 2
        val tmpY = (videoHeight - frame.height) / 2
        val nextX = videoView.x + x
        val nextY = videoView.y + y

        if (videoWidth > frame.width) {
            if (nextX <= tmpX +videoX!! && nextX >= -tmpX +videoX!!)
                videoView.x = nextX
            else if (nextX <= tmpX +videoX!!)
                videoView.x = -tmpX +videoX!!
            else
                videoView.x = tmpX +videoX!!
        }
        else if (videoX != null)
            videoView.x = videoX!!

        if (videoHeight > frame.height) {
            if (nextY <= tmpY +videoY!! && nextY >= -tmpY +videoY!!)
                videoView.y = nextY
            else if (nextY <= tmpY +videoY!!)
                videoView.y = -tmpY +videoY!!
            else
                videoView.y = tmpY +videoY!!
        }
        else if (videoY != null)
            videoView.y = videoY!!

        videoView.invalidate()
    }

    //endregion private methods

    //region public methods

    fun onPause() {
        pauseVideo()
        updateHandler?.removeCallbacks(updateProgressAction)
        timeoutHandler?.removeCallbacks(interactionTimeoutRunnable)
        lockLayout.visibility = View.VISIBLE
    }

    fun onResume() {
        lockLayout.visibility = View.GONE
        showController()
    }

    //endregion public methods

    //region override

    override fun dismiss() {
        player?.release()
        updateHandler?.removeCallbacks(updateProgressAction)
        timeoutHandler?.removeCallbacks(interactionTimeoutRunnable)

        super.dismiss()
    }

    //endregion override
}