package com.crmbl.thesafe.listeners

import com.google.android.exoplayer2.video.VideoListener


typealias OnVideoSizeChanged = (width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) -> Unit
typealias OnRenderedFirstFrame = () -> Unit

@Suppress("unused")
class ComposableVideoListener: VideoListener {
    private var onVideoSizeChanged: OnVideoSizeChanged? = null
    private var onRenderedFirstFrame: OnRenderedFirstFrame? = null

    fun onVideoSizeChanged(onVideoSizeChanged: OnVideoSizeChanged?) = this.apply {
        this.onVideoSizeChanged = onVideoSizeChanged
    }

    fun onRenderedFirstFrame(onRenderedFirstFrame: OnRenderedFirstFrame?) = this.apply {
        this.onRenderedFirstFrame = onRenderedFirstFrame
    }

    //region overriding

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        onVideoSizeChanged?.invoke(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
    }

    override fun onRenderedFirstFrame() {
        onRenderedFirstFrame?.invoke()
    }

    //endregion overriding
}