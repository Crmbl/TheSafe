package com.crmbl.thesafe.listeners

import android.view.GestureDetector
import android.view.MotionEvent

typealias OnSingleTapUp = (e: MotionEvent) -> Boolean
typealias OnLongPress = (e: MotionEvent) -> Unit
typealias OnScroll = (e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float) -> Boolean
typealias OnFling = (e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float) -> Boolean
typealias OnShowPress = (e: MotionEvent) -> Unit
typealias OnDown = (e: MotionEvent) -> Boolean
typealias OnDoubleTap = (e: MotionEvent) -> Boolean
typealias OnDoubleTapEvent = (e: MotionEvent) -> Boolean
typealias OnSingleTapConfirmed = (e: MotionEvent) -> Boolean
typealias OnContextClick = (e: MotionEvent)-> Boolean

@Suppress("unused")
class ComposableGestureListener: GestureDetector.SimpleOnGestureListener() {
    private var onSingleTapUp: OnSingleTapUp? = null
    private var onLongPress: OnLongPress? = null
    private var onScroll: OnScroll? = null
    private var onFling: OnFling? = null
    private var onShowPress: OnShowPress? = null
    private var onDown: OnDown? = null
    private var onDoubleTap: OnDoubleTap? = null
    private var onDoubleTapEvent: OnDoubleTapEvent? = null
    private var onSingleTapConfirmed: OnSingleTapConfirmed? = null
    private var onContextClick: OnContextClick? = null

    fun onSingleTapUp(onSingleTapUp: OnSingleTapUp?) = this.apply {
        this.onSingleTapUp = onSingleTapUp
    }

    fun onLongPress(onLongPress: OnLongPress?) = this.apply {
        this.onLongPress = onLongPress
    }

    fun onScroll(onScroll: OnScroll?) = this.apply {
        this.onScroll = onScroll
    }

    fun onFling(onFling: OnFling?) = this.apply {
        this.onFling = onFling
    }

    fun onShowPress(onShowPress: OnShowPress?) = this.apply {
        this.onShowPress = onShowPress
    }

    fun onDown(onDown: OnDown?) = this.apply {
        this.onDown = onDown
    }

    fun onDoubleTap(onDoubleTap: OnDoubleTap?) = this.apply {
        this.onDoubleTap = onDoubleTap
    }

    fun onDoubleTapEvent(onDoubleTapEvent: OnDoubleTapEvent?) = this.apply {
        this.onDoubleTapEvent = onDoubleTapEvent
    }

    fun onSingleTapConfirmed(onSingleTapConfirmed: OnSingleTapConfirmed?) = this.apply {
        this.onSingleTapConfirmed = onSingleTapConfirmed
    }

    fun onContextClick(onContextClick: OnContextClick?) = this.apply {
        this.onContextClick = onContextClick
    }

    //region overriding

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return if (onSingleTapUp == null) false
                else onSingleTapUp!!.invoke(e)
    }

    override fun onLongPress(e: MotionEvent) {
        onLongPress?.invoke(e)
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return if (onScroll == null) false
                else onScroll!!.invoke(e1, e2, distanceX, distanceY)

    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return if (onFling == null) false
                else onFling!!.invoke(e1, e2, velocityX, velocityY)
    }

    override fun onShowPress(e: MotionEvent) {
        onShowPress?.invoke(e)
    }

    override fun onDown(e: MotionEvent): Boolean {
        return if (onDown == null) false
                else onDown!!.invoke(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        return if (onDoubleTap == null) false
                else onDoubleTap!!.invoke(e)
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return if (onDoubleTapEvent == null) false
                else onDoubleTapEvent!!.invoke(e)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return if (onSingleTapConfirmed == null) false
                else onSingleTapConfirmed!!.invoke(e)
    }

    override fun onContextClick(e: MotionEvent): Boolean {
        return if (onContextClick == null) false
                else onContextClick!!.invoke(e)
    }

    //endregion overriding
}