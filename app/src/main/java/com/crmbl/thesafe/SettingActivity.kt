package com.crmbl.thesafe

import android.app.Activity
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.crmbl.thesafe.databinding.ActivitySettingBinding
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.transition.Fade
import android.view.View
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.crmbl.thesafe.listeners.ComposableAnimationListener
import com.crmbl.thesafe.listeners.ComposableTransitionListener
import com.crmbl.thesafe.utils.CryptoUtil
import com.crmbl.thesafe.viewModels.SettingViewModel
import kotlinx.android.synthetic.main.activity_setting.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.schedule


class SettingActivity : AppCompatActivity() {

    private var isPaused : Boolean = true
    private var onCreated : Boolean = true
    private var goMain : Boolean = false
    private var validated : Boolean = false

    private val externalRequest = 138

    private var broadcastReceiver: BroadcastReceiver? = null
    private lateinit var expand : Animation
    private lateinit var binding : ActivitySettingBinding
    private lateinit var fadeIn : Animation
    private lateinit var fadeOut : Animation
    private lateinit var theSafeFolder : DocumentFile

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAnimation()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) { val action = intent.action
                if (action == "finish_SettingActivity") { finish() }
            }}
        registerReceiver(broadcastReceiver, IntentFilter("finish_SettingActivity"))

        this.title = resources.getString(R.string.setting_field_title)
        val prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this@SettingActivity, R.layout.activity_setting)

        decryp_password_field.visibility = View.GONE
        decryp_salt_field.visibility = View.GONE

        if (prefs.firstLogin) {
            binding.viewModel = SettingViewModel()
        }
        else {
            binding.viewModel = SettingViewModel(
                prefs.rememberUsername,
                prefs.useFingerprint,
                prefs.saltDecryptHash,
                prefs.passwordDecryptHash,
                prefs.firstLogin
            )
        }

        expand = AnimationUtils.loadAnimation(applicationContext, R.anim.expand)
        fadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in)
        fadeIn.setAnimationListener(ComposableAnimationListener(onEnd = {_, _ -> }).onAnimationStart {
            decryp_password_field.visibility = View.VISIBLE
            decryp_salt_field.visibility = View.VISIBLE
        })

        fadeOut = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)
        fadeOut.setAnimationListener(ComposableAnimationListener(onEnd = {_, _ ->
            decryp_password_field.visibility = View.INVISIBLE
            decryp_salt_field.visibility = View.INVISIBLE
        }))

        setting_button_save.setOnClickListener{this.save()}
        setting_button_cancel.setOnClickListener{this.goMainActivity()}
        check_decrypt_button.setOnClickListener{this.check()}

        //TODO remove this !!!!
        binding.viewModel?.settingSalt = "EsyQJ7keJK2nkVJ8"
        binding.viewModel?.settingPassword = "3BVEnYwzN8eNTG8G"
        //////////////////////////

        if (requestForPermission())
            setTheSafeFolder(contentResolver.persistedUriPermissions.first().uri)
    }
    ////////////////////TESTING
    private fun requestForPermission(): Boolean {
        if (contentResolver.persistedUriPermissions.any())
            return true

        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val sdCard = storageManager.storageVolumes.firstOrNull { f -> !f.isPrimary }
        val volume: StorageVolume = sdCard!!
        volume.createAccessIntent(null).also { intent -> startActivityForResult(intent, externalRequest) }

        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            externalRequest -> {
                if (resultCode == Activity.RESULT_OK) {
                    contentResolver.takePersistableUriPermission(data?.data!!, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    setTheSafeFolder(data.data!!)
                } else {
                    throw Exception("The permission was not granted ! A problem occured.")
                }
                return
            }
        }
    }

    private fun setTheSafeFolder(uri: Uri) {
        //Legacy code
        /*theSafeFolder = ContextCompat.getExternalFilesDirs(applicationContext, null)
            .last{ f -> f.name == "files" && f.isDirectory }
            .listFiles().first{ f -> f.name == "Download" && f.isDirectory }
            .listFiles().first{ f -> f.name == ".blob" && f.isDirectory && f.isHidden }*/

        val wholeSD = DocumentFile.fromTreeUri(applicationContext, uri)
        val tmpFolder = wholeSD?.findFile("TheSafe")
        if (tmpFolder == null)
            wholeSD?.createDirectory("TheSafe")

        theSafeFolder = wholeSD?.findFile("TheSafe") as DocumentFile
        if (theSafeFolder.exists() && theSafeFolder.isDirectory) {
            val noMediaFile = theSafeFolder.findFile(".nomedia")
            if (noMediaFile == null)
                theSafeFolder.createFile("", ".nomedia")
        }
    }
    ////////////////////TESTING

    private fun save() {
        val viewModel : SettingViewModel = binding.viewModel!!
        if (viewModel.settingPassword.isBlank() || viewModel.settingSalt.isBlank()) {
            textview_error.text = resources.getString(R.string.setting_error_message)
            textview_error.postDelayed({ textview_error.text = "" }, 1500)
            textview_error.startAnimation(expand)
            return
        }
        if (!validated) {
            textview_error.text = resources.getString(R.string.setting_notvalidated_message)
            textview_error.postDelayed({ textview_error.text = "" }, 1500)
            textview_error.startAnimation(expand)
            return
        }
        else {
            val prefs = Prefs(this)
            textview_error.text = ""
            prefs.saltDecryptHash = viewModel.settingSalt
            prefs.passwordDecryptHash = viewModel.settingPassword
            prefs.useFingerprint = viewModel.settingUseFingerprint
            prefs.rememberUsername = viewModel.settingRememberUsername
            if (prefs.firstLogin) {
                prefs.firstLogin = false
                prefs.username = intent.getStringExtra("username")
            }

            goMainActivity()
        }
    }

    private fun check() {
        val viewModel : SettingViewModel = binding.viewModel!!

        if (!theSafeFolder.isDirectory || theSafeFolder.name != "TheSafe") {
            textview_error.text = resources.getString(R.string.setting_decrypterror_message)
            textview_error.postDelayed({ textview_error.text = "" }, 1500)
            textview_error.startAnimation(expand)
            return
        }
        else {
            CryptoUtil.password = viewModel.settingPassword
            CryptoUtil.salt = viewModel.settingSalt
            var fileToTest : DocumentFile? = null
            var fileExt = ""
            for (file in theSafeFolder.listFiles()) {
                fileExt = CryptoUtil.decipher(file.name!!).split('.')[1]
                if (fileExt == "jpg" || fileExt == "gif" || fileExt == "png") {
                    fileToTest = file
                    break
                }
            }
            if (fileToTest == null) {
                textview_error.text = resources.getString(R.string.setting_decrypterror_no_file_message)
                textview_error.postDelayed({ textview_error.text = "" }, 1500)
                textview_error.startAnimation(expand)
                return
            } else {
                val parcelFileDescriptor = applicationContext.contentResolver.openFileDescriptor(fileToTest.uri, "r")
                val output = CryptoUtil.decrypt(parcelFileDescriptor!!)
                //val testFile = File(theSafeFolder, "/testing.$fileExt")
                //val testFile = File(theSafeFolder.uri.path, "/testing.$fileExt")
                val testFile = theSafeFolder.createFile("", "/testing.$fileExt")
                //testFile.writeBytes(output!!)

                val fos: OutputStream
                fos = BufferedOutputStream(applicationContext.contentResolver.openOutputStream(testFile!!.uri, "w"))
                fos.write(output!!)
                fos.close()

                //imageview_checkup.setImageURI(Uri.fromFile(testFile))
                imageview_checkup.setImageURI(testFile.uri)
                imageview_checkup.postDelayed({
                    imageview_checkup.setImageResource(R.drawable.ic_no_encryption_background_24dp)
                    testFile.delete()
                }, 3000)
                validated = true
            }
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()

        if (!onCreated) return
        onCreated = false
        decryp_password_field.startAnimation(fadeIn)
        decryp_salt_field.startAnimation(fadeIn)
    }

    private fun goMainActivity() {
        goMain = true

        decryp_password_field.startAnimation(fadeOut)
        decryp_salt_field.startAnimation(fadeOut)
        Timer("SettingUp", false).schedule(75) {
            runOnUiThread {
                val intent = Intent(this@SettingActivity, MainActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@SettingActivity).toBundle())
            }
        }
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

        window.enterTransition.addListener(ComposableTransitionListener(onEnd = {
            var intent = Intent("finish_LoginActivity")
            sendBroadcast(intent)
            intent = Intent("finish_MainActivity")
            sendBroadcast(intent)
        }))
    }

    override fun onBackPressed() {
        if (Prefs(this).firstLogin)
            finish()
        else {
            goMainActivity()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null

        setting_button_save.setOnClickListener(null)
        setting_button_cancel.setOnClickListener(null)
        check_decrypt_button.setOnClickListener(null)
        fadeIn.setAnimationListener(null)
        fadeOut.setAnimationListener(null)
    }

    override fun onPause() {
        if (!goMain)
            layout_lock.visibility = View.VISIBLE
        goMain = false
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (isPaused) {
            layout_lock.visibility = View.GONE
            isPaused = false
            return
        }

        isPaused = true
        val intent = Intent(this@SettingActivity, LoginActivity::class.java)
        intent.putExtra("previous", "SettingActivity")
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@SettingActivity).toBundle())
    }
}