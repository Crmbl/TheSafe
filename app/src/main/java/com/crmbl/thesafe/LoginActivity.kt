package com.crmbl.thesafe

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.crmbl.thesafe.databinding.ActivityLoginBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.math.BigInteger
import java.security.MessageDigest
import android.content.Intent



class LoginActivity : AppCompatActivity() {

    var prefs : Prefs? = null
    var binding : ActivityLoginBinding? = null
    var loginCard : MaterialCardView? = null

    private var slideUp : Animation? = null
    private var slideDown : Animation? = null
    private var shake : Animation? = null
    private var slideCeiling : Animation? = null
    private var rememberUsername : Boolean = false
    private var useFingerprint : Boolean = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        prefs = Prefs(this)
        rememberUsername = prefs?.rememberUsername!!
        useFingerprint = prefs?.useFingerprint!!
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        if (rememberUsername)
            binding?.viewModel = LoginViewModel(prefs?.username!!)
        else
            binding?.viewModel = LoginViewModel("anus", "kipu")

        if (useFingerprint) {
            //var fingerprintManager : FingerprintManager = getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
        }

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
            override fun onAnimationEnd(animation: Animation?) {
                loginCard?.visibility = View.INVISIBLE
                if (rememberUsername)
                    binding?.viewModel?.username = prefs?.username!!
                else
                    binding?.viewModel?.username = ""
                binding?.viewModel?.password = ""
            }
            override fun onAnimationStart(animation: Animation?) {}
        })
        shake = AnimationUtils.loadAnimation(applicationContext, R.anim.shake)
        shake?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {}
            override fun onAnimationStart(p0: Animation?) {}
        })
        slideCeiling = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_ceiling)
        slideCeiling?.interpolator = AccelerateInterpolator()
        slideCeiling?.startOffset = 100
        slideCeiling?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                loginCard?.visibility = View.INVISIBLE

                if (prefs?.firstLogin!!) {
                    startActivity(Intent(this@LoginActivity, SettingActivity::class.java))
                    finish()
                }
                else {
                    //TODO call main activity
                    //startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    //finish()
                }
            }
            override fun onAnimationStart(p0: Animation?) {}
        })
    }

    private fun showLoginCard(value : Boolean) {
        binding?.viewModel?.isUsingFingerprint = value
        loginCard?.startAnimation(slideUp)

        if (value) {
            //TODO init fingerprint manager
        }
    }

    private fun hideLoginCard(value : Boolean) {
        binding?.viewModel?.isUsingFingerprint = value
        loginCard?.startAnimation(slideDown)
    }

    private fun login() {
        var username = binding?.viewModel?.username
        var password = binding?.viewModel?.password
        if (username?.length == 0 || password?.length == 0) { // Error handling
            loginCard?.startAnimation(shake)
        }

        //TODO remove bypass security
        //if (prefs?.usernameHash == username?.md5() && prefs?.passwordHash == password?.md5()) {
        if ("anus" == username && "kipu" == password) {
            loginCard?.startAnimation(slideCeiling)
        }
        else { // Error handling
            loginCard?.startAnimation(shake)
        }
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
    }
}
