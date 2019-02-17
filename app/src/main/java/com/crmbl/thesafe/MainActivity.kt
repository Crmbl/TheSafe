package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.github.ybq.android.spinkit.style.CubeGrid
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {

    private val loadLimit : Int = 5

    private var loadedFiles : Int = 0
    private var isPaused : Boolean = true
    private var goSettings : Boolean = false
    private var userScrolled : Boolean = false
    private var mapping : Folder? = null
    private var clickedChip : Chip? = null
    private var actualFolder : Folder? = null
    private var files : MutableList<com.crmbl.thesafe.File>? = null
    private var lastChip : Chip? = null
    private var adapter : ItemAdapter? = null

    private lateinit var lockLayout : FrameLayout
    private lateinit var emptyLayout : LinearLayout
    private lateinit var progressBar : ProgressBar
    private lateinit var bottomBar : BottomAppBar
    private lateinit var addMoreLayout : LinearLayout
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
        progressBar.indeterminateDrawable = CubeGrid()
        progressBar.visibility = View.VISIBLE
        bottomBar = findViewById(R.id.bar)
        addMoreLayout = findViewById(R.id.listview_loaditemlayout)
        lockLayout = findViewById(R.id.layout_lock)
        emptyLayout = findViewById(R.id.linearLayout_no_result)
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
                            showUi()
                        else if (initialY > finalY)
                            hideUi()
                    }
                }
                return false
            }
        })

        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    userScrolled = true
                }
            }
            override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                if (userScrolled && firstVisibleItem + visibleItemCount == totalItemCount) {
                    userScrolled = false
                    updateListView()
                }
            }
        })
        val goSettings = findViewById<ImageView>(R.id.imageview_go_settings)
        goSettings.setOnClickListener {this.goSettings()}

        val searchView = findViewById<SearchView>(R.id.searchview)
        val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText.setTextColor(resources.getColor(R.color.colorBackground, theme))
        editText.setHintTextColor(resources.getColor(R.color.colorHint, theme))

        //endregion init
        //region mapping decrypt

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

        //endregion mapping decrypt
    }

    private fun decryptMappingFile(input : ByteArray) = GlobalScope.launch {
        mapping = Klaxon().parse<Folder>(input.inputStream())
        writeParent(mapping!!)
        actualFolder = mapping!!
        decryptFiles()
    }

    private fun decryptFiles() = GlobalScope.launch {
        createChips(actualFolder!!)
        val theSafeFolder = ContextCompat.getExternalFilesDirs(applicationContext, null)[1].listFiles()[0].listFiles()[0]

        for ((i, file) in actualFolder?.files?.withIndex()!!) {
            if (i == loadLimit) break
            for (realFile in theSafeFolder.listFiles()) {
                if (file.updatedName == cryptoUtil.decipher(realFile.name.split('/').last())) {
                    file.decrypted = cryptoUtil.decrypt(realFile)
                    loadedFiles++
                }
            }
        }

        runOnUiThread {
            if (progressBar.visibility != View.GONE) {
                progressBar.animate().alpha(0f).setDuration(125).withEndAction{
                    progressBar.visibility = View.GONE
                    progressBar.alpha = 1f

                    if (listView.visibility != View.VISIBLE) {
                        listView.visibility = View.VISIBLE
                    }
                }.start()
            }

            files = if (actualFolder?.files?.count() == 0)
                        actualFolder?.files!!.toMutableList()
                    else
                        actualFolder?.files?.take(loadedFiles)!!.toMutableList()
            adapter = ItemAdapter(this@MainActivity, files!!)
            listView.adapter = adapter
            showUi()
            if (actualFolder?.files?.count() == 0) {
                emptyLayout.alpha = 0f
                emptyLayout.visibility = View.VISIBLE
                emptyLayout.animate().alpha(1f).setDuration(125).withEndAction{
                    emptyLayout.alpha = 1f
                }.start()
            }
        }
    }

    private fun updateListView() {
        if (loadedFiles != actualFolder?.files?.count()) {
            addMoreLayout.alpha = 0f
            addMoreLayout.visibility = View.VISIBLE
            addMoreLayout.animate().alpha(1f).setDuration(250).withEndAction{
                addMoreLayout.postDelayed ({
                    val theSafeFolder = ContextCompat.getExternalFilesDirs(applicationContext, null)[1].listFiles()[0].listFiles()[0]
                    for ((i, file) in actualFolder?.files?.drop(loadedFiles)?.withIndex()!!) {
                        if (i == loadLimit) break
                        for (realFile in theSafeFolder.listFiles()) {
                            if (file.updatedName == cryptoUtil.decipher(realFile.name.split('/').last())) {
                                file.decrypted = cryptoUtil.decrypt(realFile)
                                loadedFiles++
                                files?.add(file)
                            }
                        }
                    }

                    adapter?.notifyDataSetChanged()
                    addMoreLayout.visibility = View.GONE
                }, 500)
            }.start()
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

    private fun showUi() {
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

        if (mapping != null && bottomBar.visibility == View.INVISIBLE && scrollView.visibility == View.INVISIBLE) {
            bottomBar.startAnimation(slideUp)
            scrollView.startAnimation(slideDown)
        }
    }

    private fun hideUi() {
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

        if (loadedFiles == 0) return
        if (mapping != null && listView.getChildAt(listView.lastVisiblePosition - listView.firstVisiblePosition).bottom > listView.height
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
        val scrollView = findViewById<HorizontalScrollView>(R.id.scrollView_chipgroup)
        if (scrollView.visibility != View.VISIBLE) {
            navigate(false)
        } else {
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
    }

    private fun navigate(direction : Boolean) {
        emptyLayout.animate().alpha(0f).setDuration(125).withEndAction{
            emptyLayout.visibility = View.INVISIBLE
            emptyLayout.alpha = 1f
        }.start()
        listView.animate().alpha(0f).setDuration(125).withEndAction{
            listView.visibility = View.INVISIBLE
            listView.alpha = 1f
        }.start()
        progressBar.alpha = 0f
        progressBar.visibility = View.VISIBLE
        progressBar.animate().alpha(1f).setDuration(125).withEndAction{
            progressBar.alpha = 1f
        }.setStartDelay(125).start()

        chipGroup.removeAllViews()
        if (direction)
            actualFolder = findFolder(clickedChip?.text)!!
        else
            actualFolder = actualFolder?.previous?.copy()

        loadedFiles = 0
        decryptFiles()
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
            val scrollView = findViewById<HorizontalScrollView>(R.id.scrollView_chipgroup)
            scrollView.visibility = View.VISIBLE

            isPaused = false
            return
        }

        progressBar.animate().alpha(0f).setDuration(125).withEndAction{
            progressBar.visibility = View.GONE
        }.start()
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
        if (actualFolder == mapping)
            finish()
        else
            goUp()
    }
}
