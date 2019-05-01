package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.*
import android.os.Bundle
import android.transition.Fade
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crmbl.thesafe.listeners.ComposableAnimationListener
import com.crmbl.thesafe.listeners.ComposableTransitionListener
import com.crmbl.thesafe.utils.CryptoUtil
import com.crmbl.thesafe.viewHolders.ImageViewHolder
import com.crmbl.thesafe.viewHolders.ImageViewHolder.ImageViewHolderListener
import com.crmbl.thesafe.viewHolders.VideoViewHolder
import com.crmbl.thesafe.viewHolders.VideoViewHolder.VideoViewHolderListener
import com.crmbl.thesafe.viewHolders.SoundViewHolder.SoundViewHolderListener
import com.crmbl.thesafe.utils.VideoService.VideoServiceListener
import com.crmbl.thesafe.viewHolders.ScrollUpViewHolder.ScrollUpViewHolderListener
import com.github.ybq.android.spinkit.style.CubeGrid
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import android.content.Intent
import android.graphics.Rect
import android.util.DisplayMetrics
import com.crmbl.thesafe.utils.VideoService
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private val loadLimit : Int = 10

    private var loadedFiles : Int = 0
    private var isPaused : Boolean = true
    private var goSettings : Boolean = false
    private var popupDismissed : Boolean = true
    private var mapping : List<Folder>? = null
    private var clickedChip : Chip? = null
    private var actualFolder : Folder? = null
    private var previousFolder : Folder? = null
    private var previousChip : Chip? = null
    private var files : MutableList<File>? = null
    private var lastChip : Chip? = null
    private var adapter : ItemAdapter? = null
    private var fullScreen: PopupWindow? = null
    private var notificationItem: File? = null
    private var query: String = ""
    private var imageFileExtensions: Array<String> = arrayOf("gif", "png", "jpg", "jpeg", "bmp", "pdf")
    private var videoFileExtensions: Array<String> = arrayOf("webm", "mkv", "ogg", "ogv", "avi", "wmv", "mp4", "mpg", "mpeg", "m2v", "m4v", "3gp", "flv")
    private lateinit var cryptedMapping : java.io.File
    private lateinit var theSafeFolder : java.io.File

    private var broadcastReceiver: BroadcastReceiver? = null
    private var videoListener: VideoViewHolderListener? = null
    private var soundListener: SoundViewHolderListener? = null
    private var imageListener: ImageViewHolderListener? = null
    private var videoServiceListener: VideoServiceListener? = null
    private var scrollUpListener: ScrollUpViewHolderListener? = null

    //region override methods

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setAnimation()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) {
                if (intent.action == "finish_MainActivity") { finish() }
            }
        }
        registerReceiver(broadcastReceiver, IntentFilter("finish_MainActivity"))
        setContentView(R.layout.activity_main)

        val prefs = Prefs(this)
        CryptoUtil.password = prefs.passwordDecryptHash
        CryptoUtil.salt = prefs.saltDecryptHash

        progress_bar.visibility = View.VISIBLE
        progress_bar.setIndeterminateDrawable(CubeGrid())

        val layoutManager = LinearLayoutManager(this)
        recyclerview_main.layoutManager = layoutManager

        val editText = searchview.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText.setTextColor(resources.getColor(R.color.colorBackground, theme))
        editText.setHintTextColor(resources.getColor(R.color.colorHint, theme))

        theSafeFolder = ContextCompat.getExternalFilesDirs(applicationContext, null)
            .last{ f -> f.name == "files" && f.isDirectory }
            .listFiles().first{ f -> f.name == "Download" && f.isDirectory }
            .listFiles().first{ f -> f.name == ".blob" && f.isDirectory && f.isHidden }

        //region listeners

        searchview.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean { return false }
            override fun onQueryTextSubmit(query: String?): Boolean { searchQuery(query); return false }
        })

        searchview.findViewById<ImageView>(R.id.search_close_btn).setOnClickListener {
            searchview.setQuery("", false)
            searchQuery("")
        }

        recyclerview_main.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) { super.onScrolled(recyclerView, dx, dy)
                if (layoutManager.findLastCompletelyVisibleItemPosition() == files?.size!! -1) updateListView()
                if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) showUi()
                if (recyclerView.tag == "smoothScrolling") {
                    recyclerView.tag = ""
                    showUi()
                }
            }
        })

        recyclerview_main.setOnTouchListener(object : View.OnTouchListener {
            var initialY : Float = 0f
            var previouslyShown : Boolean = false

            @Suppress("DEPRECATION")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (MotionEventCompat.getActionMasked(event)) {
                    MotionEvent.ACTION_DOWN -> initialY = event?.y!!
                    MotionEvent.ACTION_UP -> {
                        if (initialY < event?.y!!) {
                            if (previouslyShown && layoutManager.findFirstCompletelyVisibleItemPosition() != 0) {
                                hideUi()
                            } else {
                                showUi()
                                previouslyShown = true
                            }
                        } else if (initialY > event.y && layoutManager.findFirstCompletelyVisibleItemPosition() != 0) {
                            hideUi()
                            previouslyShown = false
                        }
                    }
                }
                return false
            }
        })

        imageview_go_settings.setOnClickListener {goSettings()}

        videoListener = object : VideoViewHolderListener {
            override fun onFullScreenButtonClick(view: View, item: File) { showPopup(view, item) }
            override fun onBackgroundButtonClick(view: View, item: File) { runInForeground(item) }
        }
        imageListener = object: ImageViewHolderListener {
            override fun onDoubleTap(view: View, item: File) { showPopup(view, item) }
        }
        soundListener = object: SoundViewHolderListener {
            override fun onDoubleTap(view: View, item: File) { runInForeground(item) }
        }
        scrollUpListener = object: ScrollUpViewHolderListener {
            override fun onClick() {
                recyclerview_main.layoutManager!!.scrollToPosition(0)
                recyclerview_main.tag = "smoothScrolling"
            }
        }
        videoServiceListener = object: VideoServiceListener {
            override fun onServiceDestroyed() {
                if (notificationItem != null) {
                    runInForeground(notificationItem!!)
                    notificationItem = null
                }
            }
        }

        //endregion listeners

        decryptMappingFile()
    }

    override fun onResume() {
        super.onResume()

        if (isPaused) {
            if (popupDismissed) layout_lock.visibility = View.GONE
            else {
                if (fullScreen is FullScreenImage)
                    (fullScreen!! as FullScreenImage).onResume()
                if (fullScreen is FullScreenVideo)
                    (fullScreen!! as FullScreenVideo).onResume()
            }

            bar.visibility = View.VISIBLE
            scrollView_chipgroup.visibility = View.VISIBLE
            isPaused = false
            return
        }

        progress_bar.animate().alpha(0f).setDuration(125).withEndAction{ progress_bar.visibility = View.GONE }.start()
        isPaused = true
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.putExtra("previous", "MainActivity")
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@MainActivity).toBundle())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!popupDismissed) fullScreen!!.dismiss()
        unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
    }

    override fun onPause() {
        if (!goSettings) {
            bar.visibility = View.GONE
            if (popupDismissed) layout_lock.visibility = View.VISIBLE
            else {
                if (fullScreen is FullScreenImage)
                    (fullScreen!! as FullScreenImage).onPause()
                if (fullScreen is FullScreenVideo)
                    (fullScreen!! as FullScreenVideo).onPause()
            }
        }

        goSettings = false
        super.onPause()
    }

    override fun onBackPressed() {
        if (actualFolder == mapping!!.first { f -> f.name == "Origin" }) finish()
        else goBack(true)
    }

    //endregion override methods

    //region async methods

    private fun decryptMappingFile() {
        try{
            for (file in theSafeFolder.listFiles()) {
                if (CryptoUtil.decipher(file.name) == "mapping.json")
                    cryptedMapping = file
            }

            CoroutineScope(Dispatchers.Main + Job()).launch {
                val deferred = async(Dispatchers.Default) {
                    val reader = InputStreamReader(CryptoUtil.decrypt(cryptedMapping)!!.inputStream(), "UTF-8")
                    mapping = Gson().fromJson(reader, object : TypeToken<List<Folder>>() {}.type)
                }

                deferred.await()
                actualFolder = mapping!!.first { f -> f.name == "Origin" }
                decryptFiles()
            }
        }
        catch(ex : Exception) { throw Exception("Error: ${ex.message}") }
    }

    private fun decryptFiles() = CoroutineScope(Dispatchers.Main + Job()).launch {
        val actualFolderFiltered = actualFolder?.files?.filter{f-> f.originName.toLowerCase().contains(query.toLowerCase())}!!
        val deferred = async(Dispatchers.Default) {
            val displayMetrics = applicationContext.resources.displayMetrics
            val fWidth = (displayMetrics.widthPixels - Math.round(12 * displayMetrics.density))
            for ((i, file) in actualFolderFiltered.withIndex()) {
                if (i == loadLimit) break
                for (realFile in theSafeFolder.listFiles()) {
                    if (file.updatedName == CryptoUtil.decipher(realFile.name.split('/').last())) {
                        when {
                            imageFileExtensions.contains(file.updatedName.split('.').last().toLowerCase()) -> file.type = "imageView"
                            videoFileExtensions.contains(file.updatedName.split('.').last().toLowerCase()) -> file.type = "videoView"
                            else -> file.type = "soundView"
                        }

                        file.path = realFile.path
                        if (!file.frozen) {
                            val ratio: Float = fWidth / file.width.toFloat()
                            val fHeight = ratio * file.height.toFloat()

                            file.height = Math.round(fHeight).toString()
                            file.width = fWidth.toString()
                            file.frozen = true
                        }
                        loadedFiles++
                    }
                }
            }

            createChips(actualFolder!!)

            if (actualFolderFiltered.count() == 0) files = actualFolderFiltered.toMutableList()
            else { files = actualFolderFiltered.take(loadedFiles).toMutableList()
                files?.add(0, File(type="header"))
                if (loadedFiles != actualFolderFiltered.count()) files?.add(File(type="footer"))
            }
        }
        deferred.await()

        adapter = ItemAdapter(files!!, videoListener!!, imageListener!!, soundListener!!, scrollUpListener!!)
        recyclerview_main.adapter = adapter

        val filesHeight = files!!.sumBy { f -> if (f.height.isNotEmpty()) f.height.toInt() else 0 }
        if (loadedFiles == actualFolderFiltered.count() && filesHeight > recyclerview_main.height) {
            files?.add(File(type="scrollUp"))
            adapter?.notifyDataSetChanged()
        }

        if (this@MainActivity.progress_bar.visibility != View.GONE) {
            this@MainActivity.progress_bar.animate().alpha(0f).setDuration(125).withEndAction{
                this@MainActivity.progress_bar.visibility = View.GONE
                if (recyclerview_main.visibility != View.VISIBLE) recyclerview_main.visibility = View.VISIBLE

                if (actualFolderFiltered.count() == 0) {
                    this@MainActivity.linearLayout_no_result.alpha = 0f
                    this@MainActivity.linearLayout_no_result.visibility = View.VISIBLE
                    this@MainActivity.linearLayout_no_result.animate().alpha(1f).setDuration(125).withEndAction{
                        this@MainActivity.linearLayout_no_result.alpha = 1f
                        showUi()
                    }.start()
                }

                scrollUntilPreviousFolder()
            }.start()
        }
    }

    private fun scrollUntilPreviousFolder() = CoroutineScope(Dispatchers.Main + Job()).launch {
        if (previousChip != null) {
            if (!isVisible(previousChip!!)) {
                val pos = intArrayOf(0, 0)
                previousChip!!.getLocationOnScreen(pos)
                scrollView_chipgroup.scrollTo(pos.first(), 0)
            }
            previousChip = null
        }
    }

    private fun updateListView() = CoroutineScope(Dispatchers.Main + Job()).launch {
        val actualFolderFiltered = actualFolder?.files?.filter{f-> f.originName.toLowerCase().contains(query.toLowerCase())}!!
        if (loadedFiles != actualFolderFiltered.count()) {
            val displayMetrics = applicationContext.resources.displayMetrics
            val fWidth = (displayMetrics.widthPixels - Math.round(12 * displayMetrics.density))

            val deferred = async(Dispatchers.Default) {
                for ((i, file) in actualFolderFiltered.drop(loadedFiles).withIndex()) {
                    if (i == loadLimit) break
                    for (realFile in theSafeFolder.listFiles()) {
                        if (file.updatedName == CryptoUtil.decipher(realFile.name.split('/').last())) {
                            when {
                                imageFileExtensions.contains(file.updatedName.split('.').last().toLowerCase()) -> file.type = "imageView"
                                videoFileExtensions.contains(file.updatedName.split('.').last().toLowerCase()) -> file.type = "videoView"
                                else -> file.type = "soundView"
                            }

                            file.path = realFile.path
                            if (!file.frozen) {
                                val ratio: Float = fWidth / file.width.toFloat()
                                val fHeight = ratio * file.height.toFloat()

                                file.height = Math.round(fHeight).toString()
                                file.width = fWidth.toString()
                                file.frozen = true
                            }
                            loadedFiles++

                            if (files!!.size != 0 && files!!.last().type == "footer") {
                                files!!.removeAt(files!!.lastIndex)
                                runOnUiThread { adapter?.notifyItemRemoved(loadedFiles) }
                            }

                            files!!.add(file)
                            runOnUiThread { adapter?.notifyItemInserted(loadedFiles) }
                        }
                    }
                }
            }
            deferred.await()

            if (loadedFiles != actualFolderFiltered.count())
                files!!.add(File(type="footer"))
            else if (loadedFiles == actualFolderFiltered.count()
                && (recyclerview_main.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() < adapter!!.itemCount)
                files!!.add(File(type="scrollUp"))
            adapter?.notifyItemInserted(loadedFiles +1)
        }
    }

    //endregion async methods

    //region private methods

    private fun createChips(originFolder : Folder) = CoroutineScope(Dispatchers.Main + Job()).launch {
        var index = 0
        if (originFolder != mapping!!.first { f -> f.name == "Origin" }) {
            val chip = Chip(chipgroup_folders.context)
            val deferred = async(Dispatchers.Default) { chip.id = getFoldersList(actualFolder!!).count() }
            deferred.await()
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
            chip.setOnClickListener { goBack() }
            chipgroup_folders.addView(chip)
            val fadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in_no_delay)
            fadeIn.setAnimationListener(ComposableAnimationListener(_view = chip, onEnd = { _: Animation?, view:View? -> view?.alpha = 1f }))
            chip.startAnimation(fadeIn)
            index++
        }
        var foldersInOrigin = emptyList<Folder>()
        val deferred = async(Dispatchers.Default) { foldersInOrigin = getFoldersList(originFolder) }
        deferred.await()
        for ((i, folder) in foldersInOrigin.withIndex()) {
            val chip = Chip(chipgroup_folders.context)
            chip.id = i
            chip.setChipBackgroundColorResource(R.color.colorHintAccent)
            chip.setTextColor(resources.getColor(R.color.colorBackground, theme))
            chip.text = folder.name
            chip.isClickable = true
            chip.isCheckable = false
            chip.isCheckedIconVisible = false
            chip.elevation = 5f
            chip.setOnClickListener { v -> goForward(v as Chip) }
            chip.alpha = 0f
            chipgroup_folders.addView(chip)
            val fadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in_no_delay)
            fadeIn.setAnimationListener(ComposableAnimationListener(_view = chip, onEnd = { _: Animation?, view:View? -> view?.alpha = 1f }))
            chip.postDelayed({ chip.startAnimation(fadeIn) }, index * 50L)
            index++

            if (previousFolder != null && folder.name == previousFolder!!.name && folder.fullPath == previousFolder!!.fullPath) {
                previousFolder = null
                previousChip = chipgroup_folders.findViewById(chip.id)
            }
        }
    }

    private fun showUi() {
        val slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_bottombar)
        slideUp.setAnimationListener(ComposableAnimationListener(onEnd = { _: Animation?, _: View? -> bar.visibility = View.VISIBLE }))
        val slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_topbar)
        slideDown.setAnimationListener(ComposableAnimationListener(onEnd = { _: Animation?, _: View? -> scrollView_chipgroup.visibility = View.VISIBLE }))

        if (mapping != null && bar.visibility == View.INVISIBLE && scrollView_chipgroup.visibility == View.INVISIBLE) {
            bar.startAnimation(slideUp)
            scrollView_chipgroup.startAnimation(slideDown)
        }
    }

    private fun hideUi() {
        val slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_topbar)
        slideUp.setAnimationListener(ComposableAnimationListener(onEnd = { _: Animation?, _: View? -> scrollView_chipgroup.visibility = View.INVISIBLE }))
        val slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_bottombar)
        slideDown.setAnimationListener(ComposableAnimationListener(onEnd = { _: Animation?, _: View? -> bar.visibility = View.INVISIBLE }))

        if (loadedFiles == 0) return
        if (mapping != null && (recyclerview_main.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() < adapter!!.itemCount -1
            && bar.visibility == View.VISIBLE && scrollView_chipgroup.visibility == View.VISIBLE) {
            bar.startAnimation(slideDown)
            scrollView_chipgroup.startAnimation(slideUp)
        }
    }

    private fun goForward(_clickedChip : Chip) {
        clickedChip = _clickedChip

        var visibleCount = 0
        for (i in 0..chipgroup_folders.childCount) {
            val chip : Chip = chipgroup_folders.findViewById(i) ?: return
            if (!isVisible(chip)) continue
            visibleCount++

            val fadeOut = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)
            fadeOut.setAnimationListener(ComposableAnimationListener(_view = chip, onEnd = { _: Animation?, view: View? ->
                view?.visibility = View.INVISIBLE
                if (view as Chip == lastChip) {
                    lastChip = null
                    navigate(true)
                }
            }))
            chip.postDelayed({ chip.startAnimation(fadeOut) }, visibleCount * 50L)

            if (i == chipgroup_folders.childCount -1)
                lastChip = chip

            if (lastChip == null && !isVisible(chipgroup_folders.findViewById(i+1)))
                lastChip = chip
        }
    }

    private fun goBack(isBackPressed: Boolean = false) {
        if (scrollView_chipgroup.visibility != View.VISIBLE)
            navigate(false)
        else {
            var visibleCount = 0
            for (i in 0..chipgroup_folders.childCount) {
                val chip : Chip = chipgroup_folders.findViewById(i) ?: return
                if (!isVisible(chip)) continue
                visibleCount++

                val fadeOut = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)
                fadeOut.setAnimationListener(ComposableAnimationListener(_view = chip, onEnd = { _: Animation?, view: View? ->
                    view?.visibility = View.INVISIBLE
                    if (view as Chip == lastChip) {
                        lastChip = null
                        navigate(false)
                    }
                }))
                chip.postDelayed({ chip.startAnimation(fadeOut) }, visibleCount * 50L)

                if (i == chipgroup_folders.childCount -1)
                    lastChip = chip

                if (lastChip == null && isBackPressed && !isVisible(chipgroup_folders.findViewById(i+1)))
                    lastChip = chip
            }
        }
    }

    private fun isVisible(view: View): Boolean {
        if (!view.isShown) return false

        val actualPosition = Rect()
        view.getGlobalVisibleRect(actualPosition)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val screen = Rect(0, 0, width, height)

        return actualPosition.intersect(screen)
    }

    private fun searchQuery(query: String?) {
        this.query = query.orEmpty()
        navigate(null)
    }

    private fun navigate(direction : Boolean?) = CoroutineScope(Dispatchers.Main + Job()).launch {
        this@MainActivity.linearLayout_no_result.animate().alpha(0f).setDuration(125).withEndAction{
            this@MainActivity.linearLayout_no_result.visibility = View.INVISIBLE
            this@MainActivity.linearLayout_no_result.alpha = 1f
        }.start()
        recyclerview_main.animate().alpha(0f).setDuration(125).withEndAction{
            recyclerview_main.visibility = View.INVISIBLE; recyclerview_main.alpha = 1f
        }.start()

        this@MainActivity.progress_bar.animate().alpha(1f).setDuration(125)
            .withStartAction{
                this@MainActivity.progress_bar.visibility = View.VISIBLE
            }.withEndAction{
                chipgroup_folders.removeAllViews()
                if (direction != null) {
                    if (!direction) previousFolder = actualFolder
                    actualFolder = if (direction) findFolder(clickedChip?.text)!!
                                   else findParent()!!
                }

                loadedFiles = 0
                for (i in 0 until recyclerview_main.childCount) {
                    val holder = recyclerview_main.getChildViewHolder(recyclerview_main.getChildAt(i))
                    if (holder is VideoViewHolder) holder.recycleView()
                    if (holder is ImageViewHolder) holder.recycleView()
                }
                decryptFiles()
            }
        .start()
    }

    private fun findFolder(text : CharSequence?): Folder? {
        val list = getFoldersList(actualFolder!!)
        for (folder in list)
            if (folder.name == text) return folder

        throw NotImplementedError("Error, did not find a folder with name : $text in ${actualFolder?.name}")
    }

    private fun findParent(): Folder? {
        val parentPath = actualFolder!!.fullPath.removeSuffix("\\${actualFolder!!.name}")
        for (folder in mapping!!)
            if (folder.fullPath == parentPath) return folder

        throw NotImplementedError("Error, did not find a parent for folder name : ${actualFolder!!.name}")
    }

    private fun getFoldersList(origin: Folder) : List<Folder> {
        val result: MutableList<Folder> = arrayListOf()
        val t = origin.fullPath.count { c -> c == '\\' }
        for (folder in mapping!!) {
            val a = folder.fullPath.count {c -> c == '\\' }
            if (folder.fullPath.contains(origin.fullPath) && a == t + 1)
                result.add(folder)
        }

        return result
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

        window.enterTransition.addListener(ComposableTransitionListener(onEnd = {
            var intent = Intent("finish_LoginActivity")
            sendBroadcast(intent)
            intent = Intent("finish_SettingActivity")
            sendBroadcast(intent)
        }))
    }

    private fun goSettings() {
        goSettings = true
        val intent = Intent(this@MainActivity, SettingActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@MainActivity).toBundle())
    }

    private fun showPopup(view: View, file: File) {
        if (file.type != "imageView" && file.type != "videoView") return

        popupDismissed = false
        bar.clearAnimation()
        scrollView_chipgroup.clearAnimation()
        bar.visibility = View.INVISIBLE
        scrollView_chipgroup.visibility = View.INVISIBLE

        when {
            file.type == "imageView" -> fullScreen = FullScreenImage(applicationContext, view, file)
            file.type == "videoView" -> {
                pauseForegroundService()
                fullScreen = FullScreenVideo(applicationContext, view, file)
            }
        }

        val fadeIn = Fade(Fade.MODE_IN)
        fadeIn.duration = 250
        fadeIn.interpolator = AccelerateDecelerateInterpolator()
        fullScreen?.enterTransition = fadeIn
        val fadeOut = Fade(Fade.MODE_OUT)
        fadeOut.duration = 250
        fadeOut.interpolator = AccelerateDecelerateInterpolator()
        fullScreen?.exitTransition = fadeOut

        fullScreen!!.setOnDismissListener { popupDismissed = true
            fullScreen = null
            if ((recyclerview_main.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
                bar.visibility = View.VISIBLE
                scrollView_chipgroup.visibility = View.VISIBLE
            }
        }

        fullScreen!!.setTouchInterceptor(object: View.OnTouchListener{ var initialY : Float = 0f
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when {
                    event?.action == MotionEvent.ACTION_DOWN -> initialY = event.y
                    event?.action == MotionEvent.ACTION_UP -> {
                        if (Math.abs(initialY - event.y) in 1600.0..2100.0)
                            if (fullScreen is FullScreenImage && !(fullScreen as FullScreenImage).isScaling
                                || fullScreen is FullScreenVideo && !(fullScreen as FullScreenVideo).isScaling)
                                fullScreen?.dismiss()
                    }
                }
                return false
            }
        })
    }

    private fun runInForeground(item: File) {
        if (isServiceRunningInForeground(applicationContext, VideoService::class.java)) {
            val intent = Intent(this, VideoService::class.java)
            intent.action = VideoService.STOPFOREGROUND_ACTION
            Util.startForegroundService(this, intent)
            notificationItem = item
        }
        else {
            val intent = Intent(this, VideoService::class.java)
            VideoService.setMediaPath(item.path)
            VideoService.setVideoServiceListener(videoServiceListener!!)
            Util.startForegroundService(this, intent)
        }
    }

    private fun pauseForegroundService() {
        if (isServiceRunningInForeground(applicationContext, VideoService::class.java)) {
            val intent = Intent(this, VideoService::class.java)
            intent.action = VideoService.PAUSEFOREGROUND_ACTION
            Util.startForegroundService(this, intent)
        }
    }

    private fun isServiceRunningInForeground(context: Context, serviceClass: Class<VideoService>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service: ActivityManager.RunningServiceInfo in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                if (service.foreground) return true
            }
        }
        return false
    }

    //endregion private methods
}
