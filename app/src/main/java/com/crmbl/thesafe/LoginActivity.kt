package com.crmbl.thesafe

import android.annotation.SuppressLint
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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.transition.Fade
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.crmbl.thesafe.listeners.ComposableAnimationListener
import com.crmbl.thesafe.utils.StringUtil
import com.crmbl.thesafe.viewModels.LoginViewModel
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


class LoginActivity : AppCompatActivity(), FingerprintController.Callback {

    lateinit var prefs : Prefs
    lateinit var binding : ActivityLoginBinding

    private lateinit var loginCard : MaterialCardView
    private lateinit var slideUp : Animation
    private lateinit var slideDown : Animation
    private lateinit var shake : Animation
    private lateinit var expand : Animation
    private lateinit var expandXY : Animation
    private lateinit var slideLogin : Animation
    private lateinit var broadcastReceiver: BroadcastReceiver

    private var isOpen : Boolean = false
    private var rememberUsername : Boolean = false
    private var cryptoObject: FingerprintManagerCompat.CryptoObject? = null
    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null
    private val keyName = "safe_key"
    private val controller: FingerprintController by lazy {
        FingerprintController(FingerprintManagerCompat.from(applicationContext), this)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setAnimation()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) { val action = intent.action
                if (action == "finish_LoginActivity") finish() }}
        registerReceiver(broadcastReceiver, IntentFilter("finish_LoginActivity"))

        //region init binding

        this.prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        rememberUsername = prefs.rememberUsername
        val useFingerprint = prefs.useFingerprint
        if (rememberUsername)
            binding.viewModel =
                LoginViewModel(prefs.username, "", "", false, useFingerprint)
        else
            binding.viewModel = LoginViewModel("", "", "", false, useFingerprint)

        //endregion
        //region init views

        val loginButtonCancel = findViewById<MaterialButton>(R.id.login_button_cancel)
        val loginButtonText = findViewById<MaterialButton>(R.id.login_button_text)
        val loginButtonFinger = findViewById<MaterialButton>(R.id.login_button_finger)
        val loginButtonCancelF = findViewById<MaterialButton>(R.id.login_button_cancel_fingerprint)
        val loginButtonGo = findViewById<MaterialButton>(R.id.login_button_go)

        loginCard = findViewById(R.id.login_card)
        slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
        slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        shake = AnimationUtils.loadAnimation(applicationContext, R.anim.shake)
        slideLogin = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        slideLogin.interpolator = AccelerateInterpolator()
        slideLogin.startOffset = 100

        //endregion
        //region init listeners

        loginButtonText.setOnClickListener{this.showLoginCard(false)}
        loginButtonCancel.setOnClickListener{this.hideLoginCard(false)}
        loginButtonFinger.setOnClickListener{this.showLoginCard(true)}
        loginButtonCancelF.setOnClickListener{this.hideLoginCard(true)}
        loginButtonGo.setOnClickListener{this.login()}

        slideUp.setAnimationListener(ComposableAnimationListener(onEnd = { _, _ ->
            handleFingerprint()
        }).onAnimationStart {
            isOpen = true
            loginCard.visibility = View.VISIBLE
            if (rememberUsername)
                binding.viewModel?.username = prefs.username
            else
                binding.viewModel?.username = ""
            binding.viewModel?.password = ""
        })

        slideDown.setAnimationListener(ComposableAnimationListener(onEnd = { _, _ ->
            isOpen = false
            loginCard.visibility = View.INVISIBLE
        }))

        slideLogin.setAnimationListener(ComposableAnimationListener(onEnd = { _, _ ->
            isOpen = false
            loginCard.visibility = View.INVISIBLE
            handleLogin()
        }))

        expand = AnimationUtils.loadAnimation(applicationContext, R.anim.expand)
        expandXY = AnimationUtils.loadAnimation(applicationContext, R.anim.expand_xy)

        //endregion
        //region fingerprint

        try { keyStore = KeyStore.getInstance("AndroidKeyStore") }
        catch (e: KeyStoreException) { throw RuntimeException("Failed to get an instance of KeyStore", e) }

        try { keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore") }
        catch (e: NoSuchAlgorithmException) { throw RuntimeException("Failed to get an instance of KeyGenerator", e) }
        catch (e: NoSuchProviderException) { throw RuntimeException("Failed to get an instance of KeyGenerator", e) }

        createKey(this.keyName)

        val defaultCipher: Cipher
        try { defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7) }
        catch (e: NoSuchAlgorithmException) { throw RuntimeException("Failed to get an instance of Cipher", e) }
        catch (e: NoSuchPaddingException) { throw RuntimeException("Failed to get an instance of Cipher", e) }

        if (initCipher(defaultCipher, this.keyName))
            cryptoObject = FingerprintManagerCompat.CryptoObject(defaultCipher)

        //endregion
    }

    override fun onBackPressed() {
        //Do nothing ! The user must stay in here
    }

    public override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        handleFingerprint()
    }

    override fun onPause() {
        super.onPause()
        controller.stopListening()
    }

    override fun onSuccess() {
        val fingerPrintIcon = findViewById<ImageView>(R.id.imageview_fingerprint)
        fingerPrintIcon.startAnimation(expandXY)
        loginCard.startAnimation(slideLogin)
    }

    override fun onError() {
        loginCard.startAnimation(shake)
    }

    override fun onHelp() {
        val textFinger = findViewById<TextView>(R.id.textview_finger)
        binding.viewModel?.fingerMessage = resources.getString(R.string.fingerprint_help)
        textFinger.postDelayed({ binding.viewModel?.fingerMessage = "" }, 1500)
        textFinger.startAnimation(expand)
    }

    //region Private methods

    private fun showLoginCard(value : Boolean) {
        if (isOpen) return

        binding.viewModel?.isUsingFingerprint = value
        loginCard.startAnimation(slideUp)
    }

    private fun hideLoginCard(value : Boolean) {
        binding.viewModel?.isUsingFingerprint = value
        loginCard.startAnimation(slideDown)
        controller.stopListening()
    }

    private fun login() {
        val username = binding.viewModel?.username
        val password = binding.viewModel?.password
        if (username.isNullOrBlank() || password.isNullOrBlank()) { // Error handling
            loginCard.startAnimation(shake)
        }

        if (prefs.usernameHash == StringUtil().md5(username!!) && prefs.passwordHash == StringUtil().md5(password!!)) {
            loginCard.startAnimation(slideLogin)
        }
        else {
            loginCard.startAnimation(shake)
        }
    }

    @SuppressLint("PrivateResource")
    private fun handleLogin() {
        val previousActivity = intent.getStringExtra("previous")
        if (previousActivity.isNullOrBlank()) {
            val intent : Intent = if (prefs.firstLogin) {
                Intent(this@LoginActivity, SettingActivity::class.java)
            } else {
                Intent(this@LoginActivity, MainActivity::class.java)
            }
            intent.putExtra("username", binding.viewModel?.username)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@LoginActivity).toBundle())
        }
        else {
            finish()
            this.overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out)
        }
    }

    private fun handleFingerprint() {
        if (!isOpen || !binding.viewModel?.isUsingFingerprint!!) return
        cryptoObject?.let { controller.startListening(it) }
    }

    private fun initCipher(cipher: Cipher, keyName: String): Boolean {
        return try {
            keyStore?.load(null)
            val key = keyStore?.getKey(keyName, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            true
        }
        catch (e: KeyPermanentlyInvalidatedException) { false }
        catch (e: KeyStoreException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: CertificateException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: UnrecoverableKeyException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: IOException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: NoSuchAlgorithmException) { throw RuntimeException("Failed to init Cipher", e) }
        catch (e: InvalidKeyException) { throw RuntimeException("Failed to init Cipher", e) }
    }

    private fun createKey(keyName: String) {
        try {
            keyStore?.load(null)
            val builder = KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)

            keyGenerator?.init(builder.build())
            keyGenerator?.generateKey()
        }
        catch (e: NoSuchAlgorithmException) { throw RuntimeException(e) }
        catch (e: InvalidAlgorithmParameterException) { throw RuntimeException(e) }
        catch (e: CertificateException) { throw RuntimeException(e) }
        catch (e: IOException) { throw RuntimeException(e) }
    }

    private fun setAnimation() {
        val fadeIn = Fade(Fade.MODE_IN)
        fadeIn.duration = 300
        fadeIn.interpolator = AccelerateDecelerateInterpolator()
        window.enterTransition = fadeIn

        val fadeOut = Fade(Fade.MODE_OUT)
        fadeOut.duration = 300
        fadeOut.interpolator = AccelerateDecelerateInterpolator()
        window.exitTransition = fadeOut
    }

    //endregion
}
