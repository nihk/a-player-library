package player.ui.def

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.savedstate.SavedStateRegistryOwner
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
import player.ui.def.databinding.DefaultPlaybackUiBinding
import player.ui.trackspicker.TracksPickerFragment
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// todo: this should be more composable for shared components across PlaybackUis, e.g. the seekbar
class DefaultPlaybackUi(
    private val activity: FragmentActivity,
    private val navigator: Navigator,
    private val seekBarListenerFactory: SeekBarListener.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val pipController: PipController,
    private val playerController: PlayerController,
    private val playerArguments: PlayerArguments,
    private val registryOwner: SavedStateRegistryOwner,
    private val closeDelegate: CloseDelegate,
    private val shareDelegate: ShareDelegate?,
    private val timeFormatter: TimeFormatter
) : PlaybackUi {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(activity)
        .inflate(R.layout.default_playback_ui, null)

    private val binding = DefaultPlaybackUiBinding.bind(view)
    private val seekBarListener = seekBarListenerFactory.create(
        updateProgress = { position ->
            updateTimestamps(position, playerController.latestSeekData().duration)
        },
        seekTo = playerController::seekTo
    )
    private val playerViewWrapper = playerViewWrapperFactory.create(activity)

    init {
        binding.playerContainer.addView(playerViewWrapper.view)
        bindControls()
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
        if (!seekBarListener.requireNotNull().isSeekBarBeingTouched) {
            val seekData = uiState.seekData
            binding.seekBar.update(seekData)
            updateTimestamps(seekData.position, seekData.duration)
        }
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
                    binding.title.apply {
                        isVisible = true
                        text = playbackInfo.title
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
                        navigateToTracksPicker(playerController.tracks().filter { it.type == entry.value })
                    }
                }
            }
        }
    }

    private fun navigateToTracksPicker(trackInfos: List<TrackInfo>) {
        navigator.toTracksPicker(trackInfos)
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
                setOnClickListener {
                    share(activity, playerArguments.uri)
                }
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(seekBarListener)

        setPlayPause(playerController.isPlaying())
        binding.play.setOnClickListener { playerController.play() }
        binding.pause.setOnClickListener { playerController.pause() }

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
        binding.play.isVisible = !isPlaying
        binding.pause.isVisible = isPlaying
    }

    class Factory(
        private val closeDelegate: CloseDelegate,
        private val timeFormatter: TimeFormatter,
        private val shareDelegate: ShareDelegate? = null,
    ) : PlaybackUi.Factory {
        override val fragmentMap: Map<Class<out Fragment>, () -> Fragment> = mapOf(
            TracksPickerFragment::class.java to { TracksPickerFragment() }
        )

        override fun create(
            host: Fragment,
            playerViewWrapperFactory: PlayerViewWrapper.Factory,
            pipController: PipController,
            playerController: PlayerController,
            playerArguments: PlayerArguments,
            registryOwner: SavedStateRegistryOwner
        ): PlaybackUi {
            return DefaultPlaybackUi(
                activity = host.requireActivity(),
                seekBarListenerFactory = DefaultSeekBarListener.Factory(),
                navigator = Navigator(host.childFragmentManager),
                playerViewWrapperFactory = playerViewWrapperFactory,
                pipController = pipController,
                playerController = playerController,
                playerArguments = playerArguments,
                registryOwner = registryOwner,
                closeDelegate = closeDelegate,
                shareDelegate = shareDelegate,
                timeFormatter = timeFormatter
            )
        }
    }
}
