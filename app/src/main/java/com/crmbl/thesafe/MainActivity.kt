package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.transition.Fade
import android.transition.Transition
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomappbar.BottomAppBar
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import com.beust.klaxon.Klaxon
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {

    private var isPaused : Boolean = true
    private var goSettings : Boolean = false
    private var mapping : Folder? = null
    private var clickedChip : Chip? = null
    private var actualFolder : Folder? = null
    private var lastChip : Chip? = null

    private lateinit var lockLayout : FrameLayout
    private lateinit var progressBar : ProgressBar
    private lateinit var bottomBar : BottomAppBar
    private lateinit var listView : ListView
    private lateinit var chipGroup : ChipGroup
    private lateinit var prefs : Prefs
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var cryptedMapping : File
    private lateinit var cryptoUtil : CryptoUtil

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setAnimation()

        //region init

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

        progressBar = findViewById(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE
        bottomBar = findViewById(R.id.bar)
        lockLayout = findViewById(R.id.layout_lock)
        chipGroup = findViewById(R.id.chipgroup_folders)
        listView = findViewById(R.id.listview_main)
        listView.setOnTouchListener(object : View.OnTouchListener {
            var initialY : Float = 0f
            var finalY : Float = 0f

            @Suppress("DEPRECATION")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val action : Int = MotionEventCompat.getActionMasked(event)
                when (action) {
                    MotionEvent.ACTION_DOWN -> initialY = event?.y!!
                    MotionEvent.ACTION_UP -> {
                        finalY = event?.y!!

                        if (initialY < finalY)
                            scrollingUp()
                        else if (initialY > finalY)
                            scrollingDown()
                    }
                }
                return false
            }
        })
        val goSettings = findViewById<ImageView>(R.id.imageview_go_settings)
        goSettings.setOnClickListener {this.goSettings()}

        val searchView = findViewById<SearchView>(R.id.searchview)
        val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText.setTextColor(resources.getColor(R.color.colorBackground, theme))
        editText.setHintTextColor(resources.getColor(R.color.colorHint, theme))

        //endregion init
        //region listview

        try{
            cryptoUtil = CryptoUtil(prefs.passwordDecryptHash, prefs.saltDecryptHash)
            val theSafeFolder = ContextCompat.getExternalFilesDirs(this.applicationContext, null)[1].listFiles()[0].listFiles()[0]
            for (file in theSafeFolder.listFiles()) {
                if (cryptoUtil.decipher(file.name) == "mapping.json")
                    cryptedMapping = file
            }

            decryptMappingFile(cryptoUtil.decrypt(cryptedMapping)!!)
        }
        catch(ex : Exception) { throw Exception("Error: ${ex.message}") }

        //endregion listview
    }

    private fun decryptMappingFile(input : ByteArray) = GlobalScope.launch {
        mapping = Klaxon().parse<Folder>(input.inputStream())
        actualFolder = mapping!!
        writeParent(mapping!!)

        createChips(mapping!!)
        decryptFiles()

        runOnUiThread {
            progressBar.visibility = View.GONE
            val adapter = ItemAdapter(this@MainActivity, actualFolder?.files!!)
            listView.adapter = adapter
        }
    }

    private fun decryptFiles() = GlobalScope.launch {
        val theSafeFolder = ContextCompat.getExternalFilesDirs(applicationContext, null)[1].listFiles()[0].listFiles()[0]
        for (file in actualFolder?.files!!) {
            for (realFile in theSafeFolder.listFiles()) {
                if (file.updatedName == cryptoUtil.decipher(realFile.name.split('/').last())) {
                    file.decrypted = cryptoUtil.decrypt(realFile)
                    updateView(file)
                }
            }
        }
    }

    private fun updateView(file : com.crmbl.thesafe.File) {
        runOnUiThread {
            val view : View = listView.getChildAt(file.position)
            val progressBar = view.findViewById(R.id.spin_kit) as ProgressBar
            progressBar.visibility = View.GONE
            val imageView : ImageView = view.findViewById(R.id.imageView)
            val bitMap = BitmapFactory.decodeByteArray(file.decrypted, 0, file.decrypted?.size!!)
            imageView.setImageBitmap(bitMap)
        }
    }

    private fun createChips(originFolder : Folder) = GlobalScope.launch {
        runOnUiThread {
            var index = 0
            if (originFolder != mapping) {
                val chip = Chip(chipGroup.context)
                chip.id = originFolder.folders.count()
                chip.chipIcon = resources.getDrawable(R.drawable.ic_chevron_left_white_24dp, theme)
                chip.setChipBackgroundColorResource(R.color.colorAccent)
                chip.maxWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 38f, resources.displayMetrics).toInt()
                chip.chipStartPadding = 0f
                chip.iconStartPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7f, resources.displayMetrics)
                chip.isCloseIconVisible = false
                chip.isClickable = true
                chip.isCheckable = false
                chip.isCheckedIconVisible = false
                chip.elevation = 5f
                chip.alpha = 0f
                chip.setOnClickListener { goUp() }
                chipGroup.addView(chip)
                val fadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in_no_delay)
                fadeIn.setAnimationListener(object : RefAnimationListener(chip) {
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) { this.view.alpha = 1f }
                })
                chip.startAnimation(fadeIn)
                index++
            }
            for ((i, folder) in originFolder.folders.withIndex()) {
                val chip = Chip(chipGroup.context)
                chip.id = i
                chip.setChipBackgroundColorResource(R.color.colorHintAccent)
                chip.setTextColor(resources.getColor(R.color.colorBackground, theme))
                chip.text = folder.name
                chip.isClickable = true
                chip.isCheckable = false
                chip.isCheckedIconVisible = false
                chip.elevation = 5f
                chip.setOnClickListener { v -> goDown(v as Chip) }
                chip.alpha = 0f
                chipGroup.addView(chip)
                val fadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in_no_delay)
                fadeIn.setAnimationListener(object : RefAnimationListener(chip) {
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) { this.view.alpha = 1f }
                })
                chip.postDelayed({ chip.startAnimation(fadeIn) }, index * 50L)
                index++
            }
        }
    }

    private fun scrollingUp() {
        val scrollView = findViewById<HorizontalScrollView>(R.id.scrollView_chipgroup)
        val slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_bottombar)
        slideUp.setAnimationListener(object : Animation.AnimationListener { //region useless stuff
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {} //endregion
            override fun onAnimationEnd(animation: Animation?) {
                bottomBar.visibility = View.VISIBLE
            }
        })
        val slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_topbar)
        slideDown.setAnimationListener(object : Animation.AnimationListener { //region useless stuff
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {} //endregion
            override fun onAnimationEnd(animation: Animation?) {
                scrollView.visibility = View.VISIBLE
            }
        })

        if (mapping != null && actualFolder?.files?.count()!! > 1
            && bottomBar.visibility == View.INVISIBLE && scrollView.visibility == View.INVISIBLE) {
            bottomBar.startAnimation(slideUp)
            scrollView.startAnimation(slideDown)
        }
    }

    private fun scrollingDown() {
        val scrollView = findViewById<HorizontalScrollView>(R.id.scrollView_chipgroup)
        val slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_topbar)
        slideUp.setAnimationListener(object : Animation.AnimationListener { //region useless stuff
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {} //endregions
            override fun onAnimationEnd(animation: Animation?) {
                scrollView.visibility = View.INVISIBLE
            }
        })
        val slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_bottombar)
        slideDown.setAnimationListener(object : Animation.AnimationListener { //region useless stuff
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {} //endregion
            override fun onAnimationEnd(animation: Animation?) {
                bottomBar.visibility = View.INVISIBLE
            }
        })

        if (mapping != null && actualFolder?.files?.count()!! > 1
            && bottomBar.visibility == View.VISIBLE && scrollView.visibility == View.VISIBLE) {
            bottomBar.startAnimation(slideDown)
            scrollView.startAnimation(slideUp)
        }
    }

    private fun writeParent(parentFolder : Folder) {
        for (folder in parentFolder.folders) {
            folder.previous = parentFolder
            writeParent(folder)
        }
    }

    private fun goDown(_clickedChip : Chip) {
        this.clickedChip = _clickedChip

        for (i in 0..chipGroup.childCount) {
            val chip : Chip = chipGroup.findViewById(i) ?: return
            val fadeOut = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)
            fadeOut.setAnimationListener(object : RefAnimationListener(chip) {
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    this.view.visibility = View.INVISIBLE
                    if (this.view as Chip == lastChip)
                        navigate(true)
                }
            })
            chip.postDelayed({ chip.startAnimation(fadeOut) }, i * 50L)

            if (i == chipGroup.childCount -1)
                lastChip = chip
        }
    }

    private fun goUp() {
        for (i in 0..chipGroup.childCount) {
            val chip : Chip = chipGroup.findViewById(i) ?: return
            val fadeOut = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)
            fadeOut.setAnimationListener(object : RefAnimationListener(chip) {
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    this.view.visibility = View.INVISIBLE
                    if (this.view as Chip == lastChip)
                        navigate(false)
                }
            })
            chip.postDelayed({ chip.startAnimation(fadeOut) }, i * 50L)

            if (i == chipGroup.childCount -1)
                lastChip = chip
        }
    }

    private fun navigate(direction : Boolean) {
        chipGroup.removeAllViews()
        if (direction) {
            actualFolder = findFolder(clickedChip?.text)!!
            createChips(actualFolder!!)
        } else {
            actualFolder = actualFolder?.previous?.copy()
            createChips(actualFolder!!)
        }
    }

    private fun findFolder(text : CharSequence?) : Folder? {
        for (folder in actualFolder?.folders!!) {
            if (folder.name == text)
                return folder
        }

        throw NotImplementedError("Oups error :( , did not find a folder with name : $text in ${actualFolder?.name}")
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
            lockLayout.visibility = View.GONE
            bottomBar.visibility = View.VISIBLE
            isPaused = false
            return
        }

        progressBar.visibility = View.GONE
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
            bottomBar.visibility = View.GONE
            lockLayout.visibility = View.VISIBLE
        }
        goSettings = false
        super.onPause()
    }

    override fun onBackPressed() {
        finish()
    }
}
