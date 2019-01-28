package com.crmbl.thesafe

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class LoginActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        var loginButtonCancel = findViewById<MaterialButton>(R.id.login_button_cancel)
        var loginButtonText = findViewById<MaterialButton>(R.id.login_button_text)
        var loginButtonFinger = findViewById<MaterialButton>(R.id.login_button_finger)
        var loginButtonCancelF = findViewById<MaterialButton>(R.id.login_button_cancel_fingerprint)

        loginButtonText.setOnClickListener{this.showPasswordCard()}
        loginButtonCancel.setOnClickListener{this.hidePasswordCard()}
        loginButtonFinger.setOnClickListener{this.showFingerprintCard()}
        loginButtonCancelF.setOnClickListener{this.hideFingerprintCard()}
    }

    private fun showPasswordCard() {
        var loginCardPassword = findViewById<MaterialCardView>(R.id.login_card_password)
        var slideUp : Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
        loginCardPassword.visibility = View.VISIBLE
        loginCardPassword.startAnimation(slideUp)
    }

    private fun hidePasswordCard() {
        var loginCardPassword = findViewById<MaterialCardView>(R.id.login_card_password)
        var slideDown : Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        loginCardPassword.visibility = View.INVISIBLE
        loginCardPassword.startAnimation(slideDown)
    }

    private fun showFingerprintCard() {
        var loginCardFingerprint = findViewById<MaterialCardView>(R.id.login_card_fingerprint)
        var slideUp : Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
        loginCardFingerprint.visibility = View.VISIBLE
        loginCardFingerprint.startAnimation(slideUp)
    }

    private fun hideFingerprintCard() {
        var loginCardFingerprint = findViewById<MaterialCardView>(R.id.login_card_fingerprint)
        var slideDown : Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        loginCardFingerprint.visibility = View.INVISIBLE
        loginCardFingerprint.startAnimation(slideDown)
    }
}
