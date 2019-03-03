package com.crmbl.thesafe.listeners

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

typealias OnPlaybackParametersChanged = (playbackParameters: PlaybackParameters?) -> Unit
typealias OnSeekProcessed = () -> Unit
typealias OnTracksChanged = (trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) -> Unit
typealias OnPlayerError = (error: ExoPlaybackException?) -> Unit
typealias OnLoadingChanged = (isLoading: Boolean) -> Unit
typealias OnPositionDiscontinuity = (reason: Int) -> Unit
typealias OnRepeatModeChanged = (repeatMode: Int) -> Unit
typealias OnShuffleModeEnabledChanged = (shuffleModeEnabled: Boolean) -> Unit
typealias OnTimelineChanged = (timeline: Timeline?, manifest: Any?, reason: Int) -> Unit
typealias OnPlayerStateChanged = (playWhenReady: Boolean, playbackState: Int) -> Unit

@Suppress("unused")
class ComposablePlayerEventListener: Player.EventListener {
    private var onPlaybackParametersChanged: OnPlaybackParametersChanged? = null
    private var onSeekProcessed: OnSeekProcessed? = null
    private var onTracksChanged: OnTracksChanged? = null
    private var onPlayerError: OnPlayerError? = null
    private var onLoadingChanged: OnLoadingChanged? = null
    private var onPositionDiscontinuity: OnPositionDiscontinuity? = null
    private var onRepeatModeChanged: OnRepeatModeChanged? = null
    private var onShuffleModeEnabledChanged: OnShuffleModeEnabledChanged? = null
    private var onTimelineChanged: OnTimelineChanged? = null
    private var onPlayerStateChanged: OnPlayerStateChanged? = null

    fun onPlaybackParametersChanged(onPlaybackParametersChanged: OnPlaybackParametersChanged?) = this.apply {
        this.onPlaybackParametersChanged = onPlaybackParametersChanged
    }

    fun onSeekProcessed(onSeekProcessed: OnSeekProcessed?) = this.apply {
        this.onSeekProcessed = onSeekProcessed
    }

    fun onTracksChanged(onTracksChanged: OnTracksChanged?) = this.apply {
        this.onTracksChanged = onTracksChanged
    }

    fun onPlayerError(onPlayerError: OnPlayerError?) = this.apply {
        this.onPlayerError = onPlayerError
    }

    fun onLoadingChanged(onLoadingChanged: OnLoadingChanged?) = this.apply {
        this.onLoadingChanged = onLoadingChanged
    }

    fun onPositionDiscontinuity(onPositionDiscontinuity: OnPositionDiscontinuity?) = this.apply {
        this.onPositionDiscontinuity = onPositionDiscontinuity
    }

    fun onRepeatModeChanged(onRepeatModeChanged: OnRepeatModeChanged?) = this.apply {
        this.onRepeatModeChanged = onRepeatModeChanged
    }

    fun onShuffleModeEnabledChanged(onShuffleModeEnabledChanged: OnShuffleModeEnabledChanged?) = this.apply {
        this.onShuffleModeEnabledChanged = onShuffleModeEnabledChanged
    }

    fun onTimelineChanged(onTimelineChanged: OnTimelineChanged?) = this.apply {
        this.onTimelineChanged = onTimelineChanged
    }

    fun onPlayerStateChanged(onPlayerStateChanged: OnPlayerStateChanged?) = this.apply {
        this.onPlayerStateChanged = onPlayerStateChanged
    }

    //region overriding

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        this.onPlaybackParametersChanged?.invoke(playbackParameters)
    }

    override fun onSeekProcessed() {
        this.onSeekProcessed?.invoke()
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        this.onTracksChanged?.invoke(trackGroups, trackSelections)
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        this.onPlayerError?.invoke(error)
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        this.onLoadingChanged?.invoke(isLoading)
    }

    override fun onPositionDiscontinuity(reason: Int) {
        this.onPositionDiscontinuity?.invoke(reason)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        this.onRepeatModeChanged?.invoke(repeatMode)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        this.onShuffleModeEnabledChanged?.invoke(shuffleModeEnabled)
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
        this.onTimelineChanged?.invoke(timeline, manifest, reason)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        this.onPlayerStateChanged?.invoke(playWhenReady, playbackState)
    }

    //endregion overriding
}