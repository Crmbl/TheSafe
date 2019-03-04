package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.google.android.material.bottomappbar.BottomAppBar
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.crmbl.thesafe.listeners.ComposableAnimationListener
import com.crmbl.thesafe.listeners.ComposableTransitionListener
import com.crmbl.thesafe.utils.CryptoUtil
import com.crmbl.thesafe.utils.RecyclerItemClickListener
import com.crmbl.thesafe.viewHolders.ImageViewHolder
import com.crmbl.thesafe.viewHolders.VideoViewHolder
import com.github.ybq.android.spinkit.style.CubeGrid
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


//TODO improve min height, not really nice
class MainActivity : AppCompatActivity() {

    private val loadLimit : Int = 5

    private var loadedFiles : Int = 0
    private var isPaused : Boolean = true
    private var goSettings : Boolean = false
    private var popupDismissed : Boolean = true
    private var mapping : Folder? = null
    private var clickedChip : Chip? = null
    private var actualFolder : Folder? = null
    private var files : MutableList<File>? = null
    private var lastChip : Chip? = null
    private var adapter : ItemAdapter? = null
    private var fullScreen: PopupWindow? = null
    private var query: String = ""
    private var imageFileExtensions: Array<String> = arrayOf("gif", "png", "jpg", "jpeg", "bmp", "pdf")

    private lateinit var recyclerView : RecyclerView
    private lateinit var lockLayout : FrameLayout
    private lateinit var scrollView: HorizontalScrollView
    private lateinit var emptyLayout : LinearLayout
    private lateinit var progressBar : ProgressBar
    private lateinit var bottomBar : BottomAppBar
    private lateinit var searchView: SearchView
    private lateinit var chipGroup : ChipGroup
    private lateinit var prefs : Prefs
    private lateinit var layoutManager : LinearLayoutManager
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var cryptedMapping : java.io.File
    private lateinit var cryptoUtil : CryptoUtil
    private lateinit var theSafeFolder : java.io.File

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

        prefs = Prefs(this)
        scrollView = findViewById(R.id.scrollView_chipgroup)
        bottomBar = findViewById(R.id.bar)
        lockLayout = findViewById(R.id.layout_lock)
        emptyLayout = findViewById(R.id.linearLayout_no_result)
        chipGroup = findViewById(R.id.chipgroup_folders)
        searchView = bottomBar.findViewById(R.id.searchview)

        progressBar = findViewById(R.id.progress_bar)
        progressBar.indeterminateDrawable = CubeGrid()
        progressBar.visibility = View.VISIBLE

        recyclerView = findViewById(R.id.recyclerview_main)
        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val clearButton = searchView.findViewById<ImageView>(R.id.search_close_btn)
        val goSettings = findViewById<ImageView>(R.id.imageview_go_settings)
        val searchView = findViewById<SearchView>(R.id.searchview)

        val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText.setTextColor(resources.getColor(R.color.colorBackground, theme))
        editText.setHintTextColor(resources.getColor(R.color.colorHint, theme))

        theSafeFolder = ContextCompat.getExternalFilesDirs(applicationContext, null)
            .last{ f -> f.name == "files" && f.isDirectory }
            .listFiles().first{ f -> f.name == "Download" && f.isDirectory }
            .listFiles().first{ f -> f.name == ".blob" && f.isDirectory && f.isHidden }

        //region listeners

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean { return false }
            override fun onQueryTextSubmit(query: String?): Boolean { searchQuery(query); return false }
        })

        clearButton.setOnClickListener {
            searchView.setQuery("", false)
            searchQuery("")
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) { super.onScrolled(recyclerView, dx, dy)
                if (layoutManager.findLastCompletelyVisibleItemPosition() == files?.size!! -1) updateListView()
                if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) showUi()
                if (recyclerView.tag == "smoothScrolling") {
                    recyclerView.tag = ""
                    showUi()
                }
            }
        })

        recyclerView.setOnTouchListener(object : View.OnTouchListener {
            var initialY : Float = 0f
            var previouslyShown : Boolean = false

            @Suppress("DEPRECATION")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val action : Int = MotionEventCompat.getActionMasked(event)
                when (action) {
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

        recyclerView.addOnItemTouchListener(RecyclerItemClickListener(this, recyclerView,
            object: RecyclerItemClickListener.OnItemClickListener{
                override fun onLongItemClick(view: View?, position: Int) {}
                override fun onItemClick(view: View, position: Int) { showPopup(view, position) }
            })
        )

        goSettings.setOnClickListener {this.goSettings()}

        //endregion listeners

        decryptMappingFile()
    }

    override fun onResume() {
        super.onResume()

        if (isPaused) {
            if (!popupDismissed) {
                if (fullScreen is FullScreenImage)
                    (fullScreen!! as FullScreenImage).onResume()
                if (fullScreen is FullScreenVideo)
                    (fullScreen!! as FullScreenVideo).onResume()
            }
            else lockLayout.visibility = View.GONE

            bottomBar.visibility = View.VISIBLE
            scrollView.visibility = View.VISIBLE
            isPaused = false
            return
        }

        progressBar.animate().alpha(0f).setDuration(125).withEndAction{ progressBar.visibility = View.GONE }.start()
        isPaused = true
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.putExtra("previous", "MainActivity")
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@MainActivity).toBundle())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!popupDismissed) fullScreen!!.dismiss()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onPause() {
        if (!goSettings) {
            bottomBar.visibility = View.GONE
            if (!popupDismissed) {
                if (fullScreen is FullScreenImage)
                    (fullScreen!! as FullScreenImage).onPause()
                if (fullScreen is FullScreenVideo)
                    (fullScreen!! as FullScreenVideo).onPause()
            }
            else lockLayout.visibility = View.VISIBLE
        }

        goSettings = false
        super.onPause()
    }

    override fun onBackPressed() {
        if (actualFolder == mapping) finish()
        else goBack()
    }

    //endregion override methods

    //region async methods

    private fun decryptMappingFile() = GlobalScope.launch {
        try{
            cryptoUtil = CryptoUtil(prefs.passwordDecryptHash, prefs.saltDecryptHash)
            for (file in theSafeFolder.listFiles()) {
                if (cryptoUtil.decipher(file.name) == "mapping.json")
                    cryptedMapping = file
            }

            mapping = Klaxon().parse<Folder>(cryptoUtil.decrypt(cryptedMapping)!!.inputStream())
            setParent(mapping!!)
            actualFolder = mapping!!
            decryptFiles()
        }
        catch(ex : Exception) { throw Exception("Error: ${ex.message}") }
    }

    private fun decryptFiles() = GlobalScope.launch {
        createChips(actualFolder!!)
        val actualFolderFiltered = actualFolder?.files?.filter{f-> f.originName.toLowerCase().contains(query.toLowerCase())}

        for ((i, file) in actualFolderFiltered?.withIndex()!!) {
            if (i == loadLimit) break
            for (realFile in theSafeFolder.listFiles()) {
                if (file.updatedName == cryptoUtil.decipher(realFile.name.split('/').last())) {
                    if (imageFileExtensions.contains(file.updatedName.split('.').last().toLowerCase()))
                        file.type = "imageView"
                    else
                        file.type = "videoView"

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
                    if (recyclerView.visibility != View.VISIBLE) recyclerView.visibility = View.VISIBLE
                }.start()
            }

            if (actualFolderFiltered.count() == 0) files = actualFolderFiltered.toMutableList()
            else { files = actualFolderFiltered.take(loadedFiles).toMutableList()
                files?.add(0, File("", "", null, "header"))
                if (loadedFiles != actualFolderFiltered.count()) files?.add(File("", "", null, "footer"))
            }

            adapter = ItemAdapter(applicationContext, files!!, this@MainActivity)
            recyclerView.adapter = adapter
            if (loadedFiles == actualFolderFiltered.count() && recyclerView.computeVerticalScrollRange() > recyclerView.height) {
                files?.add(File("", "", null, "scrollUp"))
                adapter?.notifyDataSetChanged()
            }

            showUi()
            if (actualFolderFiltered.count() == 0) {
                emptyLayout.alpha = 0f
                emptyLayout.visibility = View.VISIBLE
                emptyLayout.animate().alpha(1f).setDuration(125).withEndAction{ emptyLayout.alpha = 1f }.start()
            }
        }
    }

    private fun updateListView() = GlobalScope.launch {
        val actualFolderFiltered = actualFolder?.files?.filter{f-> f.originName.toLowerCase().contains(query.toLowerCase())}
        if (loadedFiles != actualFolderFiltered?.count()) {
            val tmpFiles: MutableList<File> = mutableListOf()
            for ((i, file) in actualFolderFiltered?.drop(loadedFiles)?.withIndex()!!) {
                if (i == loadLimit) break
                for (realFile in theSafeFolder.listFiles()) {
                    if (file.updatedName == cryptoUtil.decipher(realFile.name.split('/').last())) {
                        if (imageFileExtensions.contains(file.updatedName.split('.').last().toLowerCase()))
                            file.type = "imageView"
                        else
                            file.type = "videoView"

                        file.decrypted = cryptoUtil.decrypt(realFile)
                        loadedFiles++
                        tmpFiles.add(file)

                        if (i == loadLimit -1 || file == actualFolderFiltered.last())
                            showFiles(tmpFiles, actualFolderFiltered)
                    }
                }
            }
        }
    }

    //endregion async methods

    //region private methods

    private fun showFiles(tmpFiles: MutableList<File>, actualFolderFiltered: List<File>) {
        if (loadedFiles != actualFolderFiltered.count())
            tmpFiles.add(File("", "", null, "footer"))
        else if (loadedFiles == actualFolderFiltered.count()
            && layoutManager.findLastCompletelyVisibleItemPosition() < adapter!!.itemCount)
            tmpFiles.add(File("", "", null, "scrollUp"))

        files?.removeAt(files!!.lastIndex)
        files?.addAll(tmpFiles)
        runOnUiThread{ adapter?.notifyDataSetChanged() }
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
                chip.setOnClickListener { goBack() }
                chipGroup.addView(chip)
                val fadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in_no_delay)
                fadeIn.setAnimationListener(ComposableAnimationListener(_view = chip, onEnd = { _: Animation?, view:View? -> view?.alpha = 1f }))
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
                chip.setOnClickListener { v -> goForward(v as Chip) }
                chip.alpha = 0f
                chipGroup.addView(chip)
                val fadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in_no_delay)
                fadeIn.setAnimationListener(ComposableAnimationListener(_view = chip, onEnd = { _: Animation?, view:View? -> view?.alpha = 1f }))
                chip.postDelayed({ chip.startAnimation(fadeIn) }, index * 50L)
                index++
            }
        }
    }

    private fun showUi() {
        val slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_bottombar)
        slideUp.setAnimationListener(ComposableAnimationListener(onEnd = { _: Animation?, _: View? -> bottomBar.visibility = View.VISIBLE }))
        val slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_topbar)
        slideDown.setAnimationListener(ComposableAnimationListener(onEnd = { _: Animation?, _: View? -> scrollView.visibility = View.VISIBLE }))

        if (mapping != null && bottomBar.visibility == View.INVISIBLE && scrollView.visibility == View.INVISIBLE) {
            bottomBar.startAnimation(slideUp)
            scrollView.startAnimation(slideDown)
        }
    }

    private fun hideUi() {
        val slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_topbar)
        slideUp.setAnimationListener(ComposableAnimationListener(onEnd = { _: Animation?, _: View? -> scrollView.visibility = View.INVISIBLE }))
        val slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_bottombar)
        slideDown.setAnimationListener(ComposableAnimationListener(onEnd = { _: Animation?, _: View? -> bottomBar.visibility = View.INVISIBLE }))

        if (loadedFiles == 0) return
        if (mapping != null && layoutManager.findLastCompletelyVisibleItemPosition() < adapter!!.itemCount -1
            && bottomBar.visibility == View.VISIBLE && scrollView.visibility == View.VISIBLE) {
            bottomBar.startAnimation(slideDown)
            scrollView.startAnimation(slideUp)
        }
    }

    private fun setParent(parentFolder : Folder) {
        for (folder in parentFolder.folders) {
            folder.parent = parentFolder
            setParent(folder)
        }
    }

    private fun goForward(_clickedChip : Chip) {
        this.clickedChip = _clickedChip

        for (i in 0..chipGroup.childCount) {
            val chip : Chip = chipGroup.findViewById(i) ?: return
            val fadeOut = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)

            fadeOut.setAnimationListener(ComposableAnimationListener(_view = chip, onEnd = { _: Animation?, view: View? ->
                view?.visibility = View.INVISIBLE
                if (view as Chip == lastChip) navigate(true)
            }))
            chip.postDelayed({ chip.startAnimation(fadeOut) }, i * 50L)

            if (i == chipGroup.childCount -1)
                lastChip = chip
        }
    }

    private fun goBack() {
        if (scrollView.visibility != View.VISIBLE)
            navigate(false)
        else {
            for (i in 0..chipGroup.childCount) {
                val chip : Chip = chipGroup.findViewById(i) ?: return
                val fadeOut = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)

                fadeOut.setAnimationListener(ComposableAnimationListener(_view = chip, onEnd = { _: Animation?, view: View? ->
                    view?.visibility = View.INVISIBLE
                    if (view as Chip == lastChip) navigate(false)
                }))
                chip.postDelayed({ chip.startAnimation(fadeOut) }, i * 50L)

                if (i == chipGroup.childCount -1)
                    lastChip = chip
            }
        }
    }

    private fun searchQuery(query: String?) {
        this.query = query.orEmpty()
        navigate(null)
    }

    private fun navigate(direction : Boolean?) {
        emptyLayout.animate().alpha(0f).setDuration(125).withEndAction{
            emptyLayout.visibility = View.INVISIBLE; emptyLayout.alpha = 1f }.start()
        recyclerView.animate().alpha(0f).setDuration(125).withEndAction{
            recyclerView.visibility = View.INVISIBLE
            recyclerView.alpha = 1f
        }.start()

        progressBar.alpha = 0f
        progressBar.visibility = View.VISIBLE
        progressBar.animate().alpha(1f).setDuration(125).withEndAction{ progressBar.alpha = 1f }.setStartDelay(125).start()

        chipGroup.removeAllViews()
        if (direction != null) {
            actualFolder = if (direction) findFolder(clickedChip?.text)!!
                            else actualFolder?.parent?.copy()
        }

        loadedFiles = 0
        recycleViews()
    }

    private fun recycleViews() = GlobalScope.launch {
        runOnUiThread {
            for (i in 0 until recyclerView.childCount) {
                val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
                if (holder is VideoViewHolder)
                    holder.recycleView()
                if (holder is ImageViewHolder)
                    holder.recycleView(this@MainActivity)
            }

            val actualFolderFiltered = actualFolder?.files?.filter{f-> f.originName.toLowerCase().contains(query.toLowerCase())}
            for (file in actualFolderFiltered!!.toList()) {
                file.decrypted = null
            }
        }
        decryptFiles()
    }

    private fun findFolder(text : CharSequence?) : Folder? {
        for (folder in actualFolder?.folders!!)
            if (folder.name == text) return folder

        throw NotImplementedError("Error, did not find a folder with name : $text in ${actualFolder?.name}")
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

    //endregion private methods

    fun showPopup(view: View, position: Int, _file: File? = null) {
        val file : File = _file ?: files!![position]
        if (file.type != "imageView" && file.type != "videoView") return

        popupDismissed = false
        bottomBar.clearAnimation()
        scrollView.clearAnimation()
        bottomBar.visibility = View.INVISIBLE
        scrollView.visibility = View.INVISIBLE

        fullScreen = null
        when {
            file.type == "imageView" -> fullScreen = FullScreenImage(applicationContext, view, file.decrypted!!, file.originName.split('.').last())
            file.type == "videoView" -> fullScreen = FullScreenVideo(applicationContext, view, file.decrypted!!)
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
            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                bottomBar.visibility = View.VISIBLE
                scrollView.visibility = View.VISIBLE
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
}
