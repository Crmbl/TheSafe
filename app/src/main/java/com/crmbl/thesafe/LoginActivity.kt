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
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.transition.Fade
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.crmbl.thesafe.listeners.ComposableAnimationListener
import com.crmbl.thesafe.utils.FingerprintController
import com.crmbl.thesafe.utils.StringUtil
import com.crmbl.thesafe.viewModels.LoginViewModel
import kotlinx.android.synthetic.main.activity_login.*
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

//TODO add app Logo !
class LoginActivity : AppCompatActivity(), FingerprintController.Callback {

    private lateinit var binding : ActivityLoginBinding
    private lateinit var slideUp : Animation
    private lateinit var slideDown : Animation
    private lateinit var shake : Animation
    private lateinit var expand : Animation
    private lateinit var expandXY : Animation
    private lateinit var slideLogin : Animation

    private var broadcastReceiver: BroadcastReceiver? = null
    private var isOpen : Boolean = false
    private var rememberUsername : Boolean = false

    private val keyName = "safe_key"
    private var cryptoObject: FingerprintManagerCompat.CryptoObject? = null
    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null
    private var controller: FingerprintController? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setAnimation()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) { val action = intent.action
                if (action == "finish_LoginActivity") { finish() }
            }}
        registerReceiver(broadcastReceiver, IntentFilter("finish_LoginActivity"))
        controller = FingerprintController(FingerprintManagerCompat.from(applicationContext), this)

        //region init binding

        val prefs = Prefs(this)
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

        slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
        slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        shake = AnimationUtils.loadAnimation(applicationContext, R.anim.shake)
        slideLogin = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        slideLogin.interpolator = AccelerateInterpolator()
        slideLogin.startOffset = 100

        //endregion
        //region init listeners

        login_button_text.setOnClickListener{this.showLoginCard(false)}
        login_button_cancel.setOnClickListener{this.hideLoginCard(false)}
        login_button_finger.setOnClickListener{this.showLoginCard(true)}
        login_button_cancel_fingerprint.setOnClickListener{this.hideLoginCard(true)}
        login_button_go.setOnClickListener{this.login()}

        slideUp.setAnimationListener(ComposableAnimationListener(onEnd = { _, _ ->
            handleFingerprint()
        }).onAnimationStart {
            isOpen = true
            login_card.visibility = View.VISIBLE
            if (rememberUsername)
                binding.viewModel?.username = prefs.username
            else
                binding.viewModel?.username = ""
            binding.viewModel?.password = ""
        })

        slideDown.setAnimationListener(ComposableAnimationListener(onEnd = { _, _ ->
            isOpen = false
            login_card.visibility = View.INVISIBLE
        }))

        slideLogin.setAnimationListener(ComposableAnimationListener(onEnd = { _, _ ->
            isOpen = false
            login_card.visibility = View.INVISIBLE
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
        broadcastReceiver = null

        login_button_text.setOnClickListener(null)
        login_button_cancel.setOnClickListener(null)
        login_button_finger.setOnClickListener(null)
        login_button_cancel_fingerprint.setOnClickListener(null)
        login_button_go.setOnClickListener(null)
        slideUp.setAnimationListener(null)
        slideDown.setAnimationListener(null)
        slideLogin.setAnimationListener(null)

        cryptoObject = null
        keyStore = null
        keyGenerator = null
        controller = null
    }

    override fun onResume() {
        super.onResume()
        handleFingerprint()
    }

    override fun onPause() {
        super.onPause()
        controller?.stopListening()
    }

    override fun onSuccess() {
        imageview_fingerprint.startAnimation(expandXY)
        login_card.startAnimation(slideLogin)
    }

    override fun onError() {
        login_card.startAnimation(shake)
    }

    override fun onHelp() {
        binding.viewModel?.fingerMessage = resources.getString(R.string.fingerprint_help)
        textview_finger.postDelayed({ binding.viewModel?.fingerMessage = "" }, 1500)
        textview_finger.startAnimation(expand)
    }

    //region Private methods

    private fun showLoginCard(value : Boolean) {
        if (isOpen) return

        binding.viewModel?.isUsingFingerprint = value
        login_card.startAnimation(slideUp)
    }

    private fun hideLoginCard(value : Boolean) {
        binding.viewModel?.isUsingFingerprint = value
        login_card.startAnimation(slideDown)
        controller?.stopListening()
    }

    private fun login() {
        val username = binding.viewModel?.username
        val password = binding.viewModel?.password
        if (username.isNullOrBlank() || password.isNullOrBlank())
            login_card.startAnimation(shake)

        val prefs = Prefs(this)
        if (prefs.usernameHash == StringUtil().md5(username!!) && prefs.passwordHash == StringUtil().md5(password!!))
            login_card.startAnimation(slideLogin)
        else
            login_card.startAnimation(shake)
    }

    @SuppressLint("PrivateResource")
    private fun handleLogin() {
        controller = null
        val previousActivity = intent.getStringExtra("previous")
        if (previousActivity.isNullOrBlank()) {
            val prefs = Prefs(this)
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
        cryptoObject?.let { controller?.startListening(it) }
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
