package com.crmbl.thesafe

import android.app.ActivityOptions
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
import android.transition.Fade
import android.transition.Transition
import android.view.View
import com.google.android.material.textfield.TextInputLayout


class SettingActivity : AppCompatActivity() {

    var prefs : Prefs? = null
    var binding : ActivitySettingBinding? = null
    private var fadeIn : Animation? = null
    private var expand : Animation? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAnimation()

        this.title = resources.getString(R.string.setting_field_title)
        prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this@SettingActivity, R.layout.activity_setting)

        var passField = findViewById<TextInputLayout>(R.id.decryp_password_field)
        var saltField = findViewById<TextInputLayout>(R.id.decryp_salt_field)
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

        var saveButton = findViewById<MaterialButton>(R.id.setting_button_save)
        saveButton.setOnClickListener{this.save()}
        var cancelButton = findViewById<MaterialButton>(R.id.setting_button_cancel)
        cancelButton.setOnClickListener{this.cancel()}
    }

    private fun save() {
        var textViewError = findViewById<TextView>(R.id.textview_error)
        val viewModel : SettingViewModel = binding?.viewModel!!
        if (viewModel.settingPassword.isBlank() || viewModel.settingSalt.isBlank()) {
            textViewError.text = resources.getString(R.string.setting_error_message)
            textViewError.startAnimation(expand)
            return
        }
        else {
            textViewError.text = ""
            prefs?.saltDecryptHash = viewModel.settingSalt
            prefs?.passwordDecryptHash = viewModel.settingPassword
            prefs?.useFingerprint = viewModel.settingUseFingerprint
            prefs?.rememberUsername = viewModel.settingRememberUsername
            if (viewModel.settingRememberUsername)
                prefs?.username = intent.getStringExtra("username")
            if (prefs?.firstLogin!!)
                prefs?.firstLogin = false

            //TODO better handling and don't forget finish setting page
            val intent = Intent(this@SettingActivity, MainActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@SettingActivity).toBundle())
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()

        val passField = findViewById<TextInputLayout>(R.id.decryp_password_field)
        val saltField = findViewById<TextInputLayout>(R.id.decryp_salt_field)
        passField.startAnimation(fadeIn)
        saltField.startAnimation(fadeIn)
    }

    private fun cancel() {
        // TODO Just cancel and get back on MainActivity
    }

    private fun setAnimation() {
        val slide = Slide(Gravity.BOTTOM)
        slide.duration = 300
        slide.interpolator = AccelerateDecelerateInterpolator()
        window.enterTransition = slide

        // TODO is it working ?
        val fade = Fade(Fade.MODE_OUT)
        fade.duration = 300
        fade.interpolator = AccelerateDecelerateInterpolator()
        window.exitTransition = fade

        window.enterTransition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition?) {
                val intent = Intent("finish_activity")
                sendBroadcast(intent)
            }
            override fun onTransitionResume(transition: Transition?) {}
            override fun onTransitionPause(transition: Transition?) {}
            override fun onTransitionCancel(transition: Transition?) {}
            override fun onTransitionStart(transition: Transition?) {}
        })
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        if (prefs?.firstLogin!!)
            finish()
    }
}