package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import android.content.Context
import com.github.chrisbanes.photoview.PhotoView
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.exo_controller_fullscreen.view.*
import java.io.IOException


@SuppressLint("ClickableViewAccessibility", "InflateParams")
class FullScreenMedia(internal var mContext: Context, v: View, imageBytes: ByteArray, fileExt : String) :
    PopupWindow((mContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
            R.layout.media_fullscreen, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {

    internal var view: View
    internal var photoView: PhotoView
    internal var loading: ProgressBar
    internal var lockLayout: FrameLayout
    internal var videoView: ZoomableExoPlayerView
    private var player: SimpleExoPlayer? = null

    init {
        elevation = 5.0f
        this.view = contentView
        val closeButton = this.view.findViewById(R.id.ib_close) as ImageButton
        isOutsideTouchable = true

        isFocusable = true
        closeButton.setOnClickListener { dismiss() }

        lockLayout = view.findViewById(R.id.layout_lock)
        photoView = view.findViewById(R.id.image)
        videoView = view.findViewById(R.id.video)
        loading = view.findViewById(R.id.loading)

        photoView.maximumScale = 6f
        loading.isIndeterminate = true
        loading.visibility = View.VISIBLE

        val imageFileExtensions: Array<String> = arrayOf("png", "jpg", "jpeg", "bmp", "pdf")
        when {
            fileExt.toLowerCase() == "gif" -> {
                photoView.visibility = View.VISIBLE
                Glide.with(mContext).asGif()
                    .load(imageBytes)
                    .listener(object : RequestListener<GifDrawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean {
                            loading.isIndeterminate = false
                            return false
                        }
                        override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            photoView.setImageDrawable(resource)
                            loading.visibility = View.GONE
                            return false
                        }
                    }).into(photoView)
            }
            imageFileExtensions.contains(fileExt.toLowerCase()) -> {
                photoView.visibility = View.VISIBLE
                Glide.with(mContext).asBitmap()
                    .load(imageBytes)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                            loading.isIndeterminate = false
                            return false
                        }
                        override fun onResourceReady(resource: Bitmap, model: Any, target: com.bumptech.glide.request.target.Target<Bitmap>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            photoView.setImageBitmap(resource)
                            loading.visibility = View.GONE
                            return false
                        }
                    }).into(photoView)
            }
            else -> {
                videoView.visibility = View.VISIBLE

                player = ExoPlayerFactory.newSimpleInstance(mContext, DefaultTrackSelector())
                player?.volume = 1f
                player?.playWhenReady = true
                videoView.player = player

                //region listeners

                player?.addVideoListener(object: VideoListener {
                    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {}
                    override fun onRenderedFirstFrame() {
                        loading.setBackgroundColor(mContext.getColor(R.color.colorItemBackground))
                        loading.visibility = View.GONE
                        videoView.showController()
                    }
                })

                videoView.exo_quit_fullscreen.setOnClickListener { this.dismiss() }
                videoView.exo_mute.setOnClickListener {
                    player?.volume = 1f
                    videoView.exo_mute.visibility = View.GONE
                    videoView.exo_volume.visibility = View.VISIBLE
                }
                videoView.exo_volume.setOnClickListener {
                    player?.volume = 0f
                    videoView.exo_mute.visibility = View.VISIBLE
                    videoView.exo_volume.visibility = View.GONE
                }
                //endregion

                val byteArrayDataSource = ByteArrayDataSource(imageBytes)
                val mediaByteUri = UriByteDataHelper().getUri(imageBytes)
                val dataSpec = DataSpec(mediaByteUri)

                try { byteArrayDataSource.open(dataSpec) } catch (e: IOException) { e.printStackTrace() }
                val factory = object : com.google.android.exoplayer2.upstream.DataSource.Factory {
                    override fun createDataSource(): com.google.android.exoplayer2.upstream.DataSource { return byteArrayDataSource } }

                val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(mediaByteUri)
                player?.prepare(mediaSource)
            }
        }

        showAtLocation(v, Gravity.CENTER, 0, 0)
    }

    override fun dismiss() {
        player?.release()
        super.dismiss()
    }
}