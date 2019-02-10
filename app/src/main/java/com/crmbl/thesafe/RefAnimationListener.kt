package com.crmbl.thesafe

import android.view.View
import android.view.animation.Animation

open class RefAnimationListener(_view : View) : Animation.AnimationListener {
    var view : View = _view
    override fun onAnimationRepeat(animation: Animation?) {}

    override fun onAnimationEnd(animation: Animation?) {}

    override fun onAnimationStart(animation: Animation?) {}

}