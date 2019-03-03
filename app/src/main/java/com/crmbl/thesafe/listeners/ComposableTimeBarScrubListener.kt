package com.crmbl.thesafe.listeners

import com.google.android.exoplayer2.ui.TimeBar

typealias OnScrubMove = (timeBar: TimeBar?, position: Long) -> Unit
typealias OnScrubStart = (timeBar: TimeBar?, position: Long) -> Unit
typealias OnScrubStop = (timeBar: TimeBar?, position: Long, canceled: Boolean) -> Unit

@Suppress("unused")
class ComposableTimeBarScrubListener(
    var onStop: OnScrubStop
): TimeBar.OnScrubListener {
    private var onScrubMove: OnScrubMove? = null
    private var onScrubStart: OnScrubStart? = null

    fun onScrubMove(onScrubMove: OnScrubMove?) = this.apply {
        this.onScrubMove = onScrubMove
    }

    fun onScrubStart(onScrubStart: OnScrubStart?) = this.apply {
        this.onScrubStart = onScrubStart
    }

    //region overriding

    override fun onScrubMove(timeBar: TimeBar?, position: Long) {
        onScrubMove?.invoke(timeBar, position)
    }

    override fun onScrubStart(timeBar: TimeBar?, position: Long) {
        onScrubStart?.invoke(timeBar, position)
    }

    override fun onScrubStop(timeBar: TimeBar?, position: Long, canceled: Boolean) {
        onStop(timeBar, position, canceled)
    }

    //endregion overriding
}