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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.github.ybq.android.spinkit.style.CubeGrid
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.exo_controller.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


//TODO add logo in app and app icon
//TODO zoom not really cool when video in "fullscreen"
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
    private var fullScreen: FullScreenMedia? = null
    private var query: String = ""

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setAnimation()

        //region init

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) {
                if (intent.action == "finish_MainActivity") { finish() }
            }
        }
        registerReceiver(broadcastReceiver, IntentFilter("finish_MainActivity"))

        prefs = Prefs(this)
        setContentView(R.layout.activity_main)

        scrollView = findViewById(R.id.scrollView_chipgroup)
        progressBar = findViewById(R.id.progress_bar)
        progressBar.indeterminateDrawable = CubeGrid()
        progressBar.visibility = View.VISIBLE
        bottomBar = findViewById(R.id.bar)
        lockLayout = findViewById(R.id.layout_lock)
        emptyLayout = findViewById(R.id.linearLayout_no_result)
        chipGroup = findViewById(R.id.chipgroup_folders)
        searchView = bottomBar.findViewById(R.id.searchview)
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean { return false }
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery(query)
                return false
            }
        })
        val clearButton = searchView.findViewById<ImageView>(R.id.search_close_btn)
        clearButton.setOnClickListener {
            searchView.setQuery("", false)
            searchQuery("")
        }

        //region RecyclerView

        recyclerView = findViewById(R.id.recyclerview_main)
        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (layoutManager.findLastCompletelyVisibleItemPosition() == files?.size!! -1)
                    updateListView()
                if (recyclerView.tag == "smoothScrolling") {
                    recyclerView.tag = ""
                    showUi()
                }
            }
        })
        recyclerView.setOnTouchListener(object : View.OnTouchListener {
            var initialY : Float = 0f
            @Suppress("DEPRECATION")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val action : Int = MotionEventCompat.getActionMasked(event)
                when (action) {
                    MotionEvent.ACTION_DOWN -> initialY = event?.y!!
                    MotionEvent.ACTION_UP -> {
                        if (initialY < event?.y!!) showUi()
                        else if (initialY > event.y) hideUi()
                    }
                }
                return false
            }
        })
        recyclerView.addOnItemTouchListener(RecyclerItemClickListener(this, recyclerView, object : RecyclerItemClickListener.OnItemClickListener {
            override fun onLongItemClick(view: View?, position: Int) {}
            override fun onItemClick(view: View, position: Int) { showPopup(view, position) }
        }))

        //endregion RecyclerView

        val goSettings = findViewById<ImageView>(R.id.imageview_go_settings)
        goSettings.setOnClickListener {this.goSettings()}
        val searchView = findViewById<SearchView>(R.id.searchview)
        val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText.setTextColor(resources.getColor(R.color.colorBackground, theme))
        editText.setHintTextColor(resources.getColor(R.color.colorHint, theme))

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

        theSafeFolder = ContextCompat.getExternalFilesDirs(applicationContext, null)
            .last{ f -> f.name == "files" && f.isDirectory }
            .listFiles().first{ f -> f.name == "Download" && f.isDirectory }
            .listFiles().first{ f -> f.name == ".blob" && f.isDirectory && f.isHidden }

        //endregion init
    }

    private fun decryptMappingFile(input : ByteArray) = GlobalScope.launch {
        mapping = Klaxon().parse<Folder>(input.inputStream())
        writeParent(mapping!!)
        actualFolder = mapping!!
        decryptFiles()
    }

    private fun decryptFiles() = GlobalScope.launch {
        createChips(actualFolder!!)
        val actualFolderFiltered = actualFolder?.files?.filter{f-> f.originName.toLowerCase().contains(query.toLowerCase())}

        for ((i, file) in actualFolderFiltered?.withIndex()!!) {
            if (i == loadLimit) break
            for (realFile in theSafeFolder.listFiles()) {
                if (file.updatedName == cryptoUtil.decipher(realFile.name.split('/').last())) {
                    //TODO error if big file, see below
                        // If file is video, then create clear file, and give the path in File object
                            // "OnChangingFolder" : delete every "clear" videos
                            // "OnPause" but not fullscreen : delete every "clear" videos // release every OnAttached videoPlayer?
                            // "OnResume" but not fullscreen : create the needed videos // check if file exists, else create it in Adapter
                            // "OnPause" and in fullscreen : store the previous position and release videoPlayer
                            // "OnResume" and in fullscreen : create the video and play from stored position
                            // "OnDestroy": delete every "clear" videos
//                    val imageFileExtensions: Array<String> = arrayOf("gif", "png", "jpg", "jpeg", "bmp", "pdf")
//                    if (imageFileExtensions.contains(file.updatedName.split('.').last().toLowerCase())) {
//                        file.decrypted = cryptoUtil.decrypt(realFile)
//                    } else {
//                        val decryptedFolder = theSafeFolder.listFiles().first{ f -> f.name == "decrypted" && f.isDirectory }
//                        val name = "${decryptedFolder.absolutePath}/${file.originName}"
//
//                        if (decryptedFolder.listFiles().none{ f -> f.name == name && f.isFile }) {
//                            val decryptedFile : java.io.File = java.io.File(name)
//                            cryptoUtil.decryptToFile(realFile, decryptedFile)
//                        }
//
//                        file.path = name
//                    }

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

                    if (recyclerView.visibility != View.VISIBLE) {
                        recyclerView.visibility = View.VISIBLE
                    }
                }.start()
            }

            if (actualFolderFiltered.count() == 0)
                files = actualFolderFiltered.toMutableList()
            else {
                files = actualFolderFiltered.take(loadedFiles).toMutableList()
                files?.add(0, File("", "", null, "header"))
                if (loadedFiles != actualFolderFiltered.count())
                    files?.add(File("", "", null, "footer"))
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
                emptyLayout.animate().alpha(1f).setDuration(125).withEndAction{
                    emptyLayout.alpha = 1f
                }.start()
            }
        }
    }

    private fun updateListView() {
        val actualFolderFiltered = actualFolder?.files?.filter{f-> f.originName.toLowerCase().contains(query.toLowerCase())}
        if (loadedFiles != actualFolderFiltered?.count()) {
          recyclerView.postDelayed ({
              files?.removeAt(files!!.lastIndex)
              for ((i, file) in actualFolderFiltered?.drop(loadedFiles)?.withIndex()!!) {
                  if (i == loadLimit) break
                  for (realFile in theSafeFolder.listFiles()) {
                      if (file.updatedName == cryptoUtil.decipher(realFile.name.split('/').last())) {
//                          val imageFileExtensions: Array<String> = arrayOf("gif", "png", "jpg", "jpeg", "bmp", "pdf")
//                          if (imageFileExtensions.contains(file.updatedName.split('.').last().toLowerCase())) {
//                              file.decrypted = cryptoUtil.decrypt(realFile)
//                          } else {
//                              val decryptedFolder = theSafeFolder.listFiles().first{ f -> f.name == "decrypted" && f.isDirectory }
//                              val name = "${decryptedFolder.absolutePath}/${file.originName}"
//
//                              if (decryptedFolder.listFiles().none{ f -> f.name == name && f.isFile }) {
//                                  val decryptedFile : java.io.File = java.io.File(name)
//                                  cryptoUtil.decryptToFile(realFile, decryptedFile)
//                              }
//
//                              file.path = name
//                          }
//                          loadedFiles++
//                          files?.add(file)

                          file.decrypted = cryptoUtil.decrypt(realFile)
                          loadedFiles++
                          files?.add(file)
                      }
                  }
              }

              if (loadedFiles != actualFolderFiltered.count())
                  files?.add(File("", "", null, "footer"))
              else if (loadedFiles == actualFolderFiltered.count()
                  && layoutManager.findLastCompletelyVisibleItemPosition() < adapter!!.itemCount)
                  files?.add(File("", "", null, "scrollUp"))

              adapter?.notifyDataSetChanged()
          }, 500)
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
        val slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_bottombar)
        slideUp.setAnimationListener(object : Animation.AnimationListener { //region useless stuff
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {} //endregion
            override fun onAnimationEnd(animation: Animation?) { bottomBar.visibility = View.VISIBLE }
        })
        val slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_topbar)
        slideDown.setAnimationListener(object : Animation.AnimationListener { //region useless stuff
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {} //endregion
            override fun onAnimationEnd(animation: Animation?) { scrollView.visibility = View.VISIBLE }
        })

        if (mapping != null && bottomBar.visibility == View.INVISIBLE && scrollView.visibility == View.INVISIBLE) {
            bottomBar.startAnimation(slideUp)
            scrollView.startAnimation(slideDown)
        }
    }

    private fun hideUi() {
        val slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_topbar)
        slideUp.setAnimationListener(object : Animation.AnimationListener { //region useless stuff
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {} //endregions
            override fun onAnimationEnd(animation: Animation?) { scrollView.visibility = View.INVISIBLE }
        })
        val slideDown = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_bottombar)
        slideDown.setAnimationListener(object : Animation.AnimationListener { //region useless stuff
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {} //endregion
            override fun onAnimationEnd(animation: Animation?) { bottomBar.visibility = View.INVISIBLE }
        })

        if (loadedFiles == 0) return
        if (mapping != null && layoutManager.findLastCompletelyVisibleItemPosition() < adapter!!.itemCount -1
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

    fun showPopup(view: View, position: Int, _file: File? = null) {
        val file : File = _file ?: files!![position]
        if (file.type != "item") return

        popupDismissed = false
        bottomBar.clearAnimation()
        scrollView.clearAnimation()
        bottomBar.visibility = View.INVISIBLE
        scrollView.visibility = View.INVISIBLE

        fullScreen = FullScreenMedia(applicationContext, view, file.decrypted!!, file.originName.split('.').last())
        val fadeIn = Fade(Fade.MODE_IN)
        fadeIn.duration = 250
        fadeIn.interpolator = AccelerateDecelerateInterpolator()
        fullScreen?.enterTransition = fadeIn

        val fadeOut = Fade(Fade.MODE_OUT)
        fadeOut.duration = 250
        fadeOut.interpolator = AccelerateDecelerateInterpolator()
        fullScreen?.exitTransition = fadeOut

        fullScreen!!.setOnDismissListener {
            popupDismissed = true
            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                if (fullScreen!!.videoView.visibility != View.GONE)
                    fullScreen!!.videoView.player.stop()

                bottomBar.visibility = View.VISIBLE
                scrollView.visibility = View.VISIBLE
            }
        }
        fullScreen!!.setTouchInterceptor(object: View.OnTouchListener{
            var initialY : Float = 0f
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when {
                    event?.action == MotionEvent.ACTION_DOWN -> initialY = event.y
                    event?.action == MotionEvent.ACTION_UP -> {
                        if (Math.abs(initialY - event.y) in 1700.0..2100.0)
                            fullScreen?.dismiss()
                    }
                }
                return false
            }
        })
    }

    private fun searchQuery(query: String?) {
        this.query = query.orEmpty()
        navigate(null)
    }

    private fun navigate(direction : Boolean?) {
        emptyLayout.animate().alpha(0f).setDuration(125).withEndAction{
            emptyLayout.visibility = View.INVISIBLE
            emptyLayout.alpha = 1f
        }.start()
        recyclerView.animate().alpha(0f).setDuration(125).withEndAction{
            recyclerView.visibility = View.INVISIBLE
            recyclerView.alpha = 1f
        }.start()
        progressBar.alpha = 0f
        progressBar.visibility = View.VISIBLE
        progressBar.animate().alpha(1f).setDuration(125).withEndAction{
            progressBar.alpha = 1f
        }.setStartDelay(125).start()

        chipGroup.removeAllViews()
        if (direction != null) {
            actualFolder = if (direction)
                findFolder(clickedChip?.text)!!
            else
                actualFolder?.previous?.copy()
        }

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
            if (!popupDismissed)
                fullScreen!!.lockLayout.visibility = View.GONE
            else
                lockLayout.visibility = View.GONE
            bottomBar.visibility = View.VISIBLE
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
        if (!popupDismissed)
            fullScreen!!.dismiss()

        unregisterReceiver(broadcastReceiver)
    }

    override fun onPause() {
        if (!goSettings) {
            bottomBar.visibility = View.GONE
            if (!popupDismissed) {
                fullScreen!!.lockLayout.visibility = View.VISIBLE
                if (fullScreen!!.videoView.visibility == View.VISIBLE)
                    fullScreen!!.videoView.exo_pause.performClick()
            }
            else
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
