package com.crmbl.thesafe

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.crmbl.thesafe.databinding.ActivitySettingBinding
import com.google.android.material.button.MaterialButton
import android.content.Intent
import android.content.IntentFilter
import android.transition.Fade
import android.transition.Transition
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.textfield.TextInputLayout


class SettingActivity : AppCompatActivity() {

    private var prefs : Prefs? = null
    private var binding : ActivitySettingBinding? = null
    private var isPaused : Boolean = true
    private var onCreated : Boolean = true
    private var fadeIn : Animation? = null
    private var expand : Animation? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var lockLayout : FrameLayout? = null
    private var goMain : Boolean = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAnimation()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) {
                val action = intent.action
                if (action == "finish_SettingActivity") {
                    finish()
                }
            }
        }
        registerReceiver(broadcastReceiver, IntentFilter("finish_SettingActivity"))

        this.title = resources.getString(R.string.setting_field_title)
        prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this@SettingActivity, R.layout.activity_setting)

        lockLayout = findViewById(R.id.layout_lock)
        val passField = findViewById<TextInputLayout>(R.id.decryp_password_field)
        val saltField = findViewById<TextInputLayout>(R.id.decryp_salt_field)
        passField.visibility = View.GONE
        saltField.visibility = View.GONE

        if (prefs?.firstLogin!!) {
            binding?.viewModel = SettingViewModel()
        }
        else {
            binding?.viewModel = SettingViewModel(
                prefs?.rememberUsername!!,
                prefs?.useFingerprint!!,
                prefs?.saltDecryptHash!!,
                prefs?.passwordDecryptHash!!,
                prefs?.firstLogin!!)
        }

        expand = AnimationUtils.loadAnimation(applicationContext, R.anim.expand)
        fadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in)
        fadeIn?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {}
            override fun onAnimationStart(p0: Animation?) {
                passField.visibility = View.VISIBLE
                saltField.visibility = View.VISIBLE
            }
        })

        val saveButton = findViewById<MaterialButton>(R.id.setting_button_save)
        saveButton.setOnClickListener{this.save()}
        val cancelButton = findViewById<MaterialButton>(R.id.setting_button_cancel)
        cancelButton.setOnClickListener{this.cancel()}
    }

    private fun save() {
        val textViewError = findViewById<TextView>(R.id.textview_error)
        val viewModel : SettingViewModel = binding?.viewModel!!
        if (viewModel.settingPassword.isBlank() || viewModel.settingSalt.isBlank()) {
            textViewError.text = resources.getString(R.string.setting_error_message)
            textViewError.postDelayed({ textViewError.text = "" }, 1500)
            textViewError.startAnimation(expand)
            return
        }
        else {
            textViewError.text = ""
            prefs?.saltDecryptHash = viewModel.settingSalt
            prefs?.passwordDecryptHash = viewModel.settingPassword
            prefs?.useFingerprint = viewModel.settingUseFingerprint
            prefs?.rememberUsername = viewModel.settingRememberUsername
            if (prefs?.firstLogin!!) {
                prefs?.firstLogin = false
                prefs?.username = intent.getStringExtra("username")
            }

            goMain = true
            val intent = Intent(this@SettingActivity, MainActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@SettingActivity).toBundle())
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()

        if (!onCreated) return
        onCreated = false
        val passField = findViewById<TextInputLayout>(R.id.decryp_password_field)
        val saltField = findViewById<TextInputLayout>(R.id.decryp_salt_field)
        passField.startAnimation(fadeIn)
        saltField.startAnimation(fadeIn)
    }

    private fun cancel() {
        goMain = true
        val intent = Intent(this@SettingActivity, MainActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@SettingActivity).toBundle())
    }

    private fun setAnimation() {
        val slide = Slide(Gravity.BOTTOM)
        slide.duration = 300
        slide.interpolator = AccelerateDecelerateInterpolator()
        window.enterTransition = slide

        val fade = Fade(Fade.MODE_OUT)
        fade.duration = 300
        fade.interpolator = AccelerateDecelerateInterpolator()
        window.exitTransition = fade

        window.enterTransition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition?) {
                var intent = Intent("finish_LoginActivity")
                sendBroadcast(intent)
                intent = Intent("finish_MainActivity")
                sendBroadcast(intent)
            }
            override fun onTransitionResume(transition: Transition?) {}
            override fun onTransitionPause(transition: Transition?) {}
            override fun onTransitionCancel(transition: Transition?) {}
            override fun onTransitionStart(transition: Transition?) {}
        })
    }

    override fun onBackPressed() {
        if (prefs?.firstLogin!!)
            finish()
        else {
            goMain = true
            val intent = Intent(this@SettingActivity, MainActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@SettingActivity).toBundle())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onPause() {
        if (!goMain)
            lockLayout?.visibility = View.VISIBLE
        goMain = false
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (isPaused) {
            lockLayout?.visibility = View.GONE
            isPaused = false
            return
        }

        isPaused = true
        val intent = Intent(this@SettingActivity, LoginActivity::class.java)
        intent.putExtra("previous", "SettingActivity")
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@SettingActivity).toBundle())
    }
}