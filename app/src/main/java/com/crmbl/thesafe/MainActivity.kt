package com.crmbl.thesafe

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import java.io.File
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import android.os.Environment
import android.transition.Fade
import android.transition.Transition
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomappbar.BottomAppBar

class MainActivity : AppCompatActivity() {

    private var prefs : Prefs? = null
    private var isPaused : Boolean = true
    private var goSettings : Boolean = false
    private var broadcastReceiver: BroadcastReceiver? = null
    private var lockLayout : FrameLayout? = null
    private var bottomBar : BottomAppBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setAnimation()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) {
                val action = intent.action
                if (action == "finish_MainActivity") {
                    finish()
                }
            }
        }
        registerReceiver(broadcastReceiver, IntentFilter("finish_MainActivity"))

        prefs = Prefs(this)
        setContentView(R.layout.activity_main)
        bottomBar = findViewById(R.id.bar)
        bottomBar?.replaceMenu(R.menu.main_menu)
        bottomBar?.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.app_bar_setting -> this.goSettings()
            }
            true
        }

        lockLayout = findViewById(R.id.layout_lock)
        //var ins: InputStream = assets.open("docd.qsp")
        //var origin : ByteArray = ins.readBytes()
        //ins.close()
        //
        //val salt = "EsyQJ7keJK2nkVJ8".toByteArray()
        //val passPhrase = "3BVEnYwzN8eNTG8G"
        //var password = PasswordDeriveBytes(passPhrase, salt, "SHA1", 2)
        //var pass32 : ByteArray = password.GetBytes(32)
        //var pass16 : ByteArray = password.GetBytes(16)
        //
        //try {
        //    val cipher = Cipher.getInstance("AES/CBC/NoPadding ")
        //    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(pass32, "SHA1PRNG"), IvParameterSpec(pass16))
        //    val decrypted = cipher.doFinal(origin)
        //
        //    val outputFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "/decrypted.gif")
        //    outputFile.writeBytes(decrypted)
        //
        //    val originFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "/crypted.gif")
        //    originFile.writeBytes(origin)
        //} catch (ex: Exception) {
        //    ex.printStackTrace()
        //}
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

        window.enterTransition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition?) {
                var intent = Intent("finish_LoginActivity")
                sendBroadcast(intent)
                intent = Intent("finish_SettingActivity")
                sendBroadcast(intent)
            }
            override fun onTransitionResume(transition: Transition?) {}
            override fun onTransitionPause(transition: Transition?) {}
            override fun onTransitionCancel(transition: Transition?) {}
            override fun onTransitionStart(transition: Transition?) {}
        })
    }

    private fun goSettings() {
        goSettings = true
        val intent = Intent(this@MainActivity, SettingActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@MainActivity).toBundle())
    }

    override fun onResume() {
        super.onResume()
        if (isPaused) {
            lockLayout?.visibility = View.GONE
            bottomBar?.visibility = View.VISIBLE
            isPaused = false
            return
        }

        isPaused = true
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.putExtra("previous", "MainActivity")
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@MainActivity).toBundle())
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onPause() {
        if (!goSettings) {
            bottomBar?.visibility = View.GONE
            lockLayout?.visibility = View.VISIBLE
        }
        goSettings = false
        super.onPause()
    }

    override fun onBackPressed() {
        finish()
    }
}
