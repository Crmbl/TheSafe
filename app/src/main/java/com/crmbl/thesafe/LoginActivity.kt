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
import androidx.appcompat.app.AlertDialog
import java.math.BigInteger
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    var prefs : Prefs? = null
    var binding : ActivityLoginBinding? = null
    var loginCard : MaterialCardView? = null

    private var slideUp : Animation? = null
    private var slideDown : Animation? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        var viewModel = LoginViewModel(prefs!!.username, "")
        binding?.viewModel = viewModel

        loginCard = findViewById(R.id.login_card)
        var loginButtonCancel = findViewById<MaterialButton>(R.id.login_button_cancel)
        var loginButtonText = findViewById<MaterialButton>(R.id.login_button_text)
        var loginButtonFinger = findViewById<MaterialButton>(R.id.login_button_finger)
        var loginButtonCancelF = findViewById<MaterialButton>(R.id.login_button_cancel_fingerprint)
        var loginButtonGo = findViewById<MaterialButton>(R.id.login_button_go)

        loginButtonText.setOnClickListener{this.showLoginCard(false)}
        loginButtonCancel.setOnClickListener{this.hideLoginCard(false)}
        loginButtonFinger.setOnClickListener{this.showLoginCard(true)}
        loginButtonCancelF.setOnClickListener{this.hideLoginCard(true)}
        loginButtonGo.setOnClickListener{this.login()}

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

        if (value) {
            //TODO init fingerprint manager
        }
    }

    private fun hideLoginCard(value : Boolean) {
        (binding?.viewModel as LoginViewModel).isUsingFingerprint = value
        loginCard?.startAnimation(slideDown)
    }

    private fun login() {
        var username = binding?.viewModel?.username
        var password = binding?.viewModel?.password

        var builder = AlertDialog.Builder(this)
        if (prefs?.usernameHash == username?.md5() && prefs?.passwordHash == password?.md5()) {
            builder.setTitle("YEAH!")
            builder.setMessage("Successfully logged in yo")
        }
        else {
            builder.setTitle("ERROR")
            builder.setMessage("Nope nope nope!!")
        }

        var dialog = builder.create()
        dialog.show()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
    }
}
