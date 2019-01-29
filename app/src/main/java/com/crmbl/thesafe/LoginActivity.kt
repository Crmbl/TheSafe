package com.crmbl.thesafe

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.crmbl.thesafe.databinding.ActivityLoginBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class LoginActivity : AppCompatActivity() {

    var binding : ActivityLoginBinding? = null
    var loginCard : MaterialCardView? = null

    private var slideUp : Animation? = null
    private var slideDown : Animation? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        var viewModel = LoginViewModel("username", "passphrase")
        binding?.viewModel = viewModel

        loginCard = findViewById(R.id.login_card)
        var loginButtonCancel = findViewById<MaterialButton>(R.id.login_button_cancel)
        var loginButtonText = findViewById<MaterialButton>(R.id.login_button_text)
        var loginButtonFinger = findViewById<MaterialButton>(R.id.login_button_finger)
        var loginButtonCancelF = findViewById<MaterialButton>(R.id.login_button_cancel_fingerprint)

        loginButtonText.setOnClickListener{this.showLoginCard(false)}
        loginButtonCancel.setOnClickListener{this.hideLoginCard(false)}
        loginButtonFinger.setOnClickListener{this.showLoginCard(true)}
        loginButtonCancelF.setOnClickListener{this.hideLoginCard(true)}

        slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
        slideUp?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {loginCard?.visibility = View.VISIBLE}
        })
        slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        slideDown?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {loginCard?.visibility = View.INVISIBLE}
            override fun onAnimationStart(animation: Animation?) {}
        })
    }

    private fun showLoginCard(value : Boolean) {
        (binding?.viewModel as LoginViewModel).isUsingFingerprint = value
        loginCard?.startAnimation(slideUp)
    }

    private fun hideLoginCard(value : Boolean) {
        (binding?.viewModel as LoginViewModel).isUsingFingerprint = value
        loginCard?.startAnimation(slideDown)
    }
}
