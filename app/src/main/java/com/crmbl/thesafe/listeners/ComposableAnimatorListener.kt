package com.crmbl.thesafe.listeners

import android.animation.Animator

typealias OnAnimatorEnd = (animation: Animator?) -> Unit
typealias OnAnimatorRepeat = (animation: Animator?) -> Unit
typealias OnAnimatorStart = (animation: Animator?) -> Unit
typealias OnAnimatorCancel = (animation: Animator?) -> Unit

@Suppress("unused")
class ComposableAnimatorListener(
    var onEnd: OnAnimatorEnd
): Animator.AnimatorListener {
    private var onRepeat: OnAnimatorRepeat? = null
    private var onStart: OnAnimatorStart? = null
    private var onCancel: OnAnimatorCancel? = null

    fun onAnimationRepeat(onRepeat: OnAnimatorRepeat?) = this.apply {
        this.onRepeat = onRepeat
    }

    fun onAnimationStart(onStart: OnAnimatorStart?) = this.apply {
        this.onStart = onStart
    }

    fun onAnimatorCancel(onCancel: OnAnimatorCancel?) = this.apply {
        this.onCancel = onCancel
    }

    //region overriding

    override fun onAnimationRepeat(animation: Animator?) {
        onRepeat?.invoke(animation)
    }

    override fun onAnimationEnd(animation: Animator?) {
        onEnd(animation)
    }

    override fun onAnimationCancel(animation: Animator?) {
        onCancel?.invoke(animation)
    }

    override fun onAnimationStart(animation: Animator?) {
        onStart?.invoke(animation)
    }

    //endregion overriding
}