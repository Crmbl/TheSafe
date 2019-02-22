package com.crmbl.thesafe

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.button.MaterialButton
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class ItemAdapter(private val context: Context, private val dataSource : MutableList<File>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val headerView = 0
    private val itemView = 1
    private val footerView = 2
    private val scrollUpView = 3
    private var lastPosition = -1
    private var mRecyclerView: RecyclerView? = null

    //region ViewHolders

    class FooterViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
    class HeaderViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
    class ScrollUpViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        private val button : MaterialButton = itemView.findViewById(R.id.button_scrollUp)

        fun bind(parent: RecyclerView?) {
            button.setOnClickListener {
                parent!!.smoothScrollToPosition(0)
                parent.tag = "smoothScrolling"
            }
        }
    }
    class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        private val textViewTitle : TextView = itemView.findViewById(R.id.textview_title)
        private val textViewExt : TextView = itemView.findViewById(R.id.textview_ext)
        private val mediaView : GifImageView = itemView.findViewById(R.id.imageView)
        private val videoView : PlayerView = itemView.findViewById(R.id.videoView)

        fun bind(file : File, mRecyclerView: RecyclerView?) {
            val splitedName = file.originName.split('.')
            textViewTitle.text = splitedName.first()
            textViewExt.text = splitedName.last()

            val imageFileExtensions: Array<String> = arrayOf("gif", "png", "jpg", "jpeg", "bmp", "pdf")
            if (imageFileExtensions.contains(splitedName.last().toLowerCase())) {
                mediaView.visibility = View.VISIBLE
                mediaView.setImageDrawable(GifDrawable(file.decrypted!!))
            }
            else if (file.originName.isNotEmpty()) {
                videoView.visibility = View.VISIBLE
                playVideo(file, mRecyclerView!!)
            }
        }

        ////////////////////////////////////////////////////////////////
        private fun playVideo(file: File, parent: RecyclerView) {
            val mCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            //val mCipher = Cipher.getInstance("AES/CBC/NoPadding")
            val prefs = Prefs(parent.context)
            val password = PasswordDeriveBytes(prefs.passwordDecryptHash, prefs.saltDecryptHash.toByteArray(Charsets.US_ASCII), "SHA1", 2)
            val pass32: ByteArray = password.getBytes(32)
            val pass16: ByteArray = password.getBytes(16)
            mCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(pass32, "SHA1PRNG"), IvParameterSpec(pass16))

            val bandwidthMeter = DefaultBandwidthMeter()
            val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
            val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
            val player = ExoPlayerFactory.newSimpleInstance(parent.context, trackSelector)
            videoView.player = player
            val dataSourceFactory = EncryptedFileDataSourceFactory(mCipher, SecretKeySpec(pass32, "SHA1PRNG"), IvParameterSpec(pass16), bandwidthMeter)
            //val extractorsFactory = DefaultExtractorsFactory()
            try {
                val uri = Uri.fromFile(java.io.File(file.path))
                //val videoSource = ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null)
                val videoSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
                player.prepare(videoSource)
                player.playWhenReady = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ////////////////////////////////////////////////////////////////

        fun clearAnimation() {
            itemView.clearAnimation()
        }
    }

    //endregion

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            headerView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item_header, parent, false)
                return HeaderViewHolder(view)
            }
            itemView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
                view.clipToOutline = true
                return ItemViewHolder(view)
            }
            footerView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item_footer, parent, false)
                return FooterViewHolder(view)
            }
            scrollUpView -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item_scrollup, parent, false)
                return ScrollUpViewHolder(view)
            }
        }

        throw Exception("This viewType has not been defined : $viewType")
    }

    override fun getItemViewType(position: Int): Int {
        if (dataSource[position].type == "header")
            return headerView
        if (dataSource[position].type == "footer")
            return footerView
        if (dataSource[position].type == "scrollUp")
            return scrollUpView

        return itemView
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                val file : File = dataSource[position]
                holder.bind(file, mRecyclerView)
                setAnimation(holder.itemView, position)
            }
            is FooterViewHolder -> {
                setAnimation(holder.itemView, position)
            }
            is ScrollUpViewHolder -> {
                holder.bind(mRecyclerView)
                setAnimation(holder.itemView, position)
            }
            is HeaderViewHolder -> {}
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is ItemViewHolder)
            holder.clearAnimation()
    }

    private fun setAnimation(itemView: View, position: Int) {
        if (position > lastPosition) {
            val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down)
            itemView.startAnimation(animation)
            lastPosition = position
        }
    }
}