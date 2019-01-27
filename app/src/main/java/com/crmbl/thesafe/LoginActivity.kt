package com.crmbl.thesafe

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class LoginActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

//        var viewModel = LoginViewModel()
//        viewModel.username = "Test"
//        viewModel.password = "Plouf"

        var loginButtonText = findViewById<MaterialButton>(R.id.login_button_text)
        loginButtonText.setOnClickListener{this.showPasswordCard()}

        var loginButtonCancel = findViewById<MaterialButton>(R.id.login_button_cancel)
        loginButtonCancel.setOnClickListener{this.hidePasswordCard()}
    }

    private fun showPasswordCard() {
        var buttonsLayout = findViewById<LinearLayout>(R.id.buttons_layout)
        var loginCardPassword = findViewById<MaterialCardView>(R.id.login_card_password)
        var buttonsLayoutPos = buttonsLayout.y + buttonsLayout.height
        loginCardPassword.y = buttonsLayoutPos - loginCardPassword.height

//        add slide up effect
//        var slideUp : Animation = AnimationUtils.
        loginCardPassword.visibility = View.VISIBLE
    }

    private fun hidePasswordCard() {
        var loginCardPassword = findViewById<MaterialCardView>(R.id.login_card_password)
        loginCardPassword.visibility = View.INVISIBLE
    }
}
