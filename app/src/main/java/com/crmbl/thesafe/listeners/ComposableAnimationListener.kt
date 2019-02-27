package com.crmbl.thesafe.listeners

import android.view.View
import android.view.animation.Animation

typealias OnAnimationEnd = (animation: Animation?, view: View?) -> Unit
typealias OnAnimationRepeat = (animation: Animation?) -> Unit
typealias OnAnimationStart = (animation: Animation?) -> Unit

@Suppress("unused")
class ComposableAnimationListener(
    var onEnd: OnAnimationEnd,
    var _view : View? = null
): Animation.AnimationListener {
    var view : View? = _view
    private var onRepeat: OnAnimationRepeat? = null
    private var onStart: OnAnimationStart? = null

    fun onAnimationRepeat(onRepeat: OnAnimationRepeat?) = this.apply {
        this.onRepeat = onRepeat
    }

    fun onAnimationStart(onStart: OnAnimationStart?) = this.apply {
        this.onStart = onStart
    }

    //region overriding

    override fun onAnimationEnd(animation: Animation?) {
        onEnd(animation, view)
    }

    override fun onAnimationRepeat(animation: Animation?) {
        onRepeat?.invoke(animation)
    }

    override fun onAnimationStart(animation: Animation?) {
        onStart?.invoke(animation)
    }

    //endregion overriding
}