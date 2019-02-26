package com.crmbl.thesafe.listeners

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.transition.TransitionManager
import android.view.ViewPropertyAnimator

class test {

    inline fun ViewPropertyAnimator.setListener(
        crossinline animationStart: (Animator) -> Unit,
        crossinline animationRepeat: (Animator) -> Unit,
        crossinline animationCancel: (Animator) -> Unit,
        crossinline animationEnd: (Animator) -> Unit) {

        setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                animationStart(animation)
            }

            override fun onAnimationRepeat(animation: Animator) {
                animationRepeat(animation)
            }

            override fun onAnimationCancel(animation: Animator) {
                animationCancel(animation)
            }

            override fun onAnimationEnd(animation: Animator) {
                animationEnd(animation)
            }
        })
    }


    /*inline fun TransitionManager.setListener(
        crossinline transitionEnd: (Transition?) -> Unit,
        crossinline transitionResume: (Transition?) -> Unit,
        crossinline transitionPause: (Transition?) -> Unit,
        crossinline transitionCancel: (Transition?) -> Unit,
        crossinline transitionStart: (Transition?) -> Unit) {

        setListener(object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition?) {
                transitionEnd(transition)
            }

            override fun onTransitionResume(transition: Transition?) {
                transitionResume(transition)
            }

            override fun onTransitionPause(transition: Transition?) {
                transitionPause(transition)
            }

            override fun onTransitionCancel(transition: Transition?) {
                transitionCancel(transition)
            }

            override fun onTransitionStart(transition: Transition?) {
                transitionStart(transition)
            }
        })
    }*/

}

