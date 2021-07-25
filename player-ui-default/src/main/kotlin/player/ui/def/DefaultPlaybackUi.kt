package player.ui.def

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerViewWrapper
import player.common.SeekData
import player.common.TrackInfo
import player.common.requireNotNull
import player.ui.common.CloseDelegate
import player.ui.common.DefaultSeekBarListener
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.common.PlayerController
import player.ui.common.SeekBarListener
import player.ui.common.ShareDelegate
import player.ui.common.TimeFormatter
import player.ui.common.TracksState
import player.ui.common.UiState
import player.ui.common.setOnSingleClickListener
import player.ui.def.databinding.DefaultPlaybackUiBinding
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// fixme: why is seekbar thumb ripple drawable activating when clicking on the default_container parent?
// todo: this should be more composable for shared components across PlaybackUis, e.g. the seekbar
class DefaultPlaybackUi(
    private val activity: ComponentActivity,
    private val navigator: Navigator,
    private val seekBarListenerFactory: SeekBarListener.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val pipController: PipController,
    private val playerController: PlayerController,
    private val playerArguments: PlayerArguments,
    private val closeDelegate: CloseDelegate,
    private val shareDelegate: ShareDelegate?,
    private val timeFormatter: TimeFormatter,
    private val tracksPickerConfigFactory: TracksPickerConfigFactory
) : PlaybackUi {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(activity)
        .inflate(R.layout.default_playback_ui, null)

    private val binding = DefaultPlaybackUiBinding.bind(view)
    private val seekBarListener = seekBarListenerFactory.create(
        updateProgress = { position ->
            updateTimestamps(position, playerController.latestSeekData().duration)
        },
        seekTo = playerController::seekTo,
        onTrackingTouchChanged = { isTracking ->
            binding.fadingContainer.setFadingEnabled(!isTracking && playerController.isPlaying())
        }
    )
    private val playerViewWrapper = playerViewWrapperFactory.create(activity)
    private var activeTracksPickerType: TrackInfo.Type? = null
    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                activity.savedStateRegistry.registerSavedStateProvider(PROVIDER, this)
            }
        }
    }

    init {
        activity.lifecycle.addObserver(observer)
        view.doOnAttach {
            // Nested because otherwise it will be called immediately, before View is attached.
            view.doOnDetach {
                activity.lifecycle.removeObserver(observer)
                val isPlayerClosed = !activity.isChangingConfigurations
                if (isPlayerClosed) {
                    activity.savedStateRegistry.unregisterSavedStateProvider(PROVIDER)
                }
            }
        }
        binding.playerContainer.addView(playerViewWrapper.view)
        bindControls()
        restoreState()
    }

    override fun attach(appPlayer: AppPlayer) {
        playerViewWrapper.attach(appPlayer)
    }

    override fun detachPlayer() {
        playerViewWrapper.detachPlayer()
    }

    override fun onPlayerEvent(playerEvent: PlayerEvent) {
        playerViewWrapper.onEvent(playerEvent)
        when (playerEvent) {
            is PlayerEvent.Initial -> setPlayPause(playerController.isPlaying())
            is PlayerEvent.OnIsPlayingChanged -> setPlayPause(playerEvent.isPlaying)
        }
    }

    override fun onUiState(uiState: UiState) {
        binding.playerController.isVisible = uiState.isControllerUsable && !pipController.isInPip()
        binding.progressBar.isVisible = uiState.showLoading
    }

    override fun onSeekData(seekData: SeekData) {
        if (seekBarListener.requireNotNull().isSeekBarBeingTouched) return

        binding.seekBar.update(seekData)
        updateTimestamps(seekData.position, seekData.duration)
    }

    override fun onTracksState(tracksState: TracksState) {
        if (tracksState is TracksState.Available) {
            bindTracksToPicker(tracksState)
        }
    }

    override fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>) {
        playbackInfos.forEach { playbackInfo ->
            when (playbackInfo) {
                is PlaybackInfo.MediaTitle -> {
                    if (playbackInfo.mediaUriRef == playerArguments.uri) {
                        binding.title.apply {
                            isVisible = true
                            text = playbackInfo.title
                        }
                    }
                }
            }
        }
    }

    private fun bindTracksToPicker(available: TracksState.Available) {
        val typesToBind = mapOf(
            binding.videoTracks to TrackInfo.Type.VIDEO,
            binding.audioTracks to TrackInfo.Type.AUDIO,
            binding.textTracks to TrackInfo.Type.TEXT
        )
        typesToBind.forEach { entry ->
            if (entry.value in available.trackTypes) {
                entry.key.apply {
                    isVisible = true
                    setOnClickListener {
                        navigateToTracksPicker(entry.value)
                    }
                }
            }
        }
    }

    private fun navigateToTracksPicker(type: TrackInfo.Type) {
        // Keep things visible in the background of the dialog - it's a bit less distracting.
        binding.fadingContainer.setFadingEnabled(false)
        activeTracksPickerType = type
        val config = tracksPickerConfigFactory.create(type)
        navigator.toTracksPicker(config) {
            activeTracksPickerType = null
            binding.fadingContainer.setFadingEnabled(playerController.isPlaying())
        }
    }

    private fun SeekBar.update(seekData: SeekData) {
        max = seekData.duration.inWholeSeconds.toInt()
        progress = seekData.position.inWholeSeconds.toInt()
        secondaryProgress = seekData.buffered.inWholeSeconds.toInt()
    }

    private fun bindControls() {
        shareDelegate?.run {
            binding.share.apply {
                isVisible = true
                setOnSingleClickListener {
                    share(activity, playerArguments.uri)
                }
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(seekBarListener)

        setPlayPause(playerController.isPlaying())
        binding.playPause.setOnClickListener { view ->
            if (view.isSelected) {
                playerController.pause()
                // It's generally a good UX to keep all controls visible while in a paused state.
                binding.fadingContainer.setFadingEnabled(false)
            } else {
                playerController.play()
                binding.fadingContainer.setFadingEnabled(true)
            }
        }

        binding.seekBackward.setOnClickListener {
            val amount = -playerArguments.seekConfiguration.backwardAmount.toDuration(DurationUnit.MILLISECONDS)
            playerController.seekRelative(amount)
        }
        binding.seekForward.setOnClickListener {
            val amount = playerArguments.seekConfiguration.forwardAmount.toDuration(DurationUnit.MILLISECONDS)
            playerController.seekRelative(amount)
        }

        binding.close.setOnClickListener {
            closeDelegate.onClose(activity)
        }
    }

    private fun updateTimestamps(
        position: Duration,
        duration: Duration
    ) {
        binding.position.text = timeFormatter.playerTime(position)
        binding.remaining.text = timeFormatter.playerTime(duration - position)
        // todo: content descriptions
    }

    private fun setPlayPause(isPlaying: Boolean) {
        binding.playPause.isSelected = isPlaying
        val a11y = if (isPlaying) R.string.pause else R.string.play
        binding.playPause.contentDescription = activity.getString(a11y)
    }

    private fun restoreState() {
        val state = activity.savedStateRegistry.consumeRestoredStateForKey(PROVIDER)
        val type = state?.getSerializable(KEY_ACTIVE_TRACKS_PICKER_TYPE) as? TrackInfo.Type
            ?: return
        navigateToTracksPicker(type)
    }

    override fun saveState(): Bundle {
        return bundleOf(KEY_ACTIVE_TRACKS_PICKER_TYPE to activeTracksPickerType)
    }

    companion object {
        private const val PROVIDER = "default_playback_ui"
        private const val KEY_ACTIVE_TRACKS_PICKER_TYPE = "active_tracks_picker_type"
    }

    class Factory(
        private val closeDelegate: CloseDelegate,
        private val timeFormatter: TimeFormatter,
        private val shareDelegate: ShareDelegate? = null,
    ) : PlaybackUi.Factory {
        override fun create(
            activity: ComponentActivity,
            playerViewWrapperFactory: PlayerViewWrapper.Factory,
            pipController: PipController,
            playerController: PlayerController,
            playerArguments: PlayerArguments
        ): PlaybackUi {
            return DefaultPlaybackUi(
                activity = activity,
                seekBarListenerFactory = DefaultSeekBarListener.Factory(),
                navigator = Navigator(activity, playerController),
                playerViewWrapperFactory = playerViewWrapperFactory,
                pipController = pipController,
                playerController = playerController,
                playerArguments = playerArguments,
                closeDelegate = closeDelegate,
                shareDelegate = shareDelegate,
                timeFormatter = timeFormatter,
                tracksPickerConfigFactory = TracksPickerConfigFactory()
            )
        }
    }
}
