package com.crmbl.thesafe.listeners

import android.transition.Transition

typealias OnTransitionEnd = (transition: Transition?) -> Unit
typealias OnTransitionResume = (transition: Transition?) -> Unit
typealias OnTransitionPause = (transition: Transition?) -> Unit
typealias OnTransitionCancel = (transition: Transition?) -> Unit
typealias OnTransitionStart = (transition: Transition?) -> Unit

@Suppress("unused")
class ComposableTransitionListener(
    var onEnd: OnTransitionEnd
): Transition.TransitionListener {
    private var onResume: OnTransitionResume? = null
    private var onPause: OnTransitionPause? = null
    private var onCancel: OnTransitionCancel? = null
    private var onStart: OnTransitionStart? = null

    fun onTransitionResume(onResume: OnTransitionResume?) = this.apply {
        this.onResume = onResume
    }

    fun onTransitionPause(onPause: OnTransitionPause?) = this.apply {
        this.onPause = onPause
    }

    fun onTransitionCancel(onCancel: OnTransitionCancel?) = this.apply {
        this.onCancel = onCancel
    }

    fun onTransitionStart(onStart: OnTransitionStart?) = this.apply {
        this.onStart = onStart
    }

    //region overriding

    override fun onTransitionEnd(transition: Transition?) {
        onEnd(transition)
    }

    override fun onTransitionResume(transition: Transition?) {
        onResume?.invoke(transition)
    }

    override fun onTransitionPause(transition: Transition?) {
        onPause?.invoke(transition)
    }

    override fun onTransitionCancel(transition: Transition?) {
        onCancel?.invoke(transition)
    }

    override fun onTransitionStart(transition: Transition?) {
        onStart?.invoke(transition)
    }

    //endregion overriding
}