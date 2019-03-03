package com.crmbl.thesafe.listeners

import android.view.ScaleGestureDetector

typealias OnScaleEnd = (detector: ScaleGestureDetector) -> Unit
typealias OnScaleBegin = (detector: ScaleGestureDetector) -> Boolean
typealias OnScale = (detector: ScaleGestureDetector) -> Boolean

@Suppress("unused")
class ComposableScaleGestureListener: ScaleGestureDetector.SimpleOnScaleGestureListener() {
    private var onScaleEnd: OnScaleEnd? = null
    private var onScaleBegin: OnScaleBegin? = null
    private var onScale: OnScale? = null

    fun onScaleEnd(onScaleEnd: OnScaleEnd?) = this.apply {
        this.onScaleEnd = onScaleEnd
    }

    fun onScaleBegin(onScaleBegin: OnScaleBegin?) = this.apply {
        this.onScaleBegin = onScaleBegin
    }

    fun onScale(onScale: OnScale?) = this.apply {
        this.onScale = onScale
    }

    //region overriding

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        onScaleEnd?.invoke(detector)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return if (onScaleBegin == null) true
               else onScaleBegin!!.invoke(detector)
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        return if (onScale == null) false
                else onScale!!.invoke(detector)
    }

    //endregion overriding
}