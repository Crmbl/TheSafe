package com.crmbl.thesafe

import android.app.ActivityOptions
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
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context


class LoginActivity : AppCompatActivity() {

    var prefs : Prefs? = null
    var binding : ActivityLoginBinding? = null
    var loginCard : MaterialCardView? = null

    private var isOpen : Boolean = false
    private var slideUp : Animation? = null
    private var slideDown : Animation? = null
    private var shake : Animation? = null
    private var slideLogging : Animation? = null
    private var rememberUsername : Boolean = false
    private var useFingerprint : Boolean = false
    private var broadcastReceiver: BroadcastReceiver? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) {
                val action = intent.action
                if (action == "finish_activity") {
                    finish()
                }
            }
        }
        registerReceiver(broadcastReceiver, IntentFilter("finish_activity"))

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
            override fun onAnimationStart(animation: Animation?) {
                isOpen = true
                loginCard?.visibility = View.VISIBLE
            }
        })
        slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        slideDown?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                isOpen = false
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
        slideLogging = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        slideLogging?.interpolator = AccelerateInterpolator()
        slideLogging?.startOffset = 100
        slideLogging?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                isOpen = false
                loginCard?.visibility = View.INVISIBLE

                if (prefs?.firstLogin!!) {
                    var intent = Intent(this@LoginActivity, SettingActivity::class.java)
                    intent.putExtra("username", binding?.viewModel?.username)
                    //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@LoginActivity).toBundle())
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

    @Override
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun showLoginCard(value : Boolean) {
        if (isOpen) return

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
        if (username.isNullOrBlank() || password.isNullOrBlank()) { // Error handling
            loginCard?.startAnimation(shake)
        }

        //TODO remove bypass security
//        if (prefs?.usernameHash == StringUtil().md5(username!!) && prefs?.passwordHash == StringUtil().md5(password!!)) {
        if ("anus" == username && "kipu" == password) {
            loginCard?.startAnimation(slideLogging)
        }
        else { // Error handling
            loginCard?.startAnimation(shake)
        }
    }
}
