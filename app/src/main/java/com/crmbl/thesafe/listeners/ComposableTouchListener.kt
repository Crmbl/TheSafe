package com.crmbl.thesafe.listeners

import android.view.MotionEvent
import android.view.View

typealias OnTouch = (v: View?, event: MotionEvent?) -> Boolean

@Suppress("unused")
class ComposableTouchListener(
    var onTouchMyDick: OnTouch
): View.OnTouchListener {
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return onTouchMyDick(v, event)
    }
}