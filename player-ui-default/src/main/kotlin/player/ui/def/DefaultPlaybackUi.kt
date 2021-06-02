package player.ui.def

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import player.common.PlayerEvent
import player.common.SeekData
import player.common.TrackInfo
import player.common.requireNotNull
import player.ui.def.databinding.DefaultPlaybackUiBinding
import player.ui.shared.PlaybackUi
import player.ui.shared.PlayerController
import player.ui.shared.SharedDependencies
import player.ui.shared.TracksState
import player.ui.shared.UiState
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// todo: this should be more composable for shared components across PlaybackUis, e.g. the seekbar
class DefaultPlaybackUi(
    private val deps: SharedDependencies,
    private val playerController: PlayerController,
) : PlaybackUi {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(deps.context)
        .inflate(R.layout.default_playback_ui, null)

    private val binding = DefaultPlaybackUiBinding.bind(view)
    private val seekBarListener = deps.seekBarListenerFactory.create(
        updateProgress = { position ->
            updateTimestamps(position, playerController.latestSeekData().duration)
        },
        seekTo = playerController::seekTo
    )

    init {
        bindControls()
    }

    override fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.Initial -> setPlayPause(playerController.isPlaying())
            is PlayerEvent.OnIsPlayingChanged -> setPlayPause(playerEvent.isPlaying)
        }
    }

    override fun onUiState(uiState: UiState) {
        binding.root.isVisible = uiState.isControllerUsable && !deps.pipController.isInPip()
        if (!seekBarListener.requireNotNull().isSeekBarBeingTouched) {
            val seekData = uiState.seekData
            binding.seekBar.update(seekData)
            updateTimestamps(seekData.position, seekData.duration)
        }
        binding.title.apply {
            isVisible = uiState.title != null
            text = uiState.title
        }
    }

    override fun onTracksState(tracksState: TracksState) {
        if (tracksState is TracksState.Available) {
            bindTracksToPicker(tracksState)
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
        deps.navigator.toTracksPicker(trackInfos)
    }

    private fun SeekBar.update(seekData: SeekData) {
        max = seekData.duration.inWholeSeconds.toInt()
        progress = seekData.position.inWholeSeconds.toInt()
        secondaryProgress = seekData.buffered.inWholeSeconds.toInt()
    }

    private fun bindControls() {
        deps.shareDelegate?.run {
            binding.share.apply {
                isVisible = true
                setOnClickListener {
                    share(deps.context, deps.playerArguments.uri)
                }
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(seekBarListener)

        setPlayPause(playerController.isPlaying())
        binding.playPause.setOnClickListener {
            if (playerController.isPlaying()) {
                playerController.pause()
            } else {
                playerController.play()
            }
        }

        binding.seekBackward.setOnClickListener {
            val amount = -deps.playerArguments.seekConfiguration.backwardAmount.toDuration(DurationUnit.MILLISECONDS)
            playerController.seekRelative(amount)
        }
        binding.seekForward.setOnClickListener {
            val amount = deps.playerArguments.seekConfiguration.forwardAmount.toDuration(DurationUnit.MILLISECONDS)
            playerController.seekRelative(amount)
        }
    }

    private fun updateTimestamps(
        position: Duration,
        duration: Duration
    ) {
        binding.position.text = deps.timeFormatter.playerTime(position)
        binding.remaining.text = deps.timeFormatter.playerTime(duration - position)
        // todo: content descriptions
    }

    private fun setPlayPause(isPlaying: Boolean) {
        val resource = if (isPlaying) {
            R.drawable.pause
        } else {
            R.drawable.play
        }
        binding.playPause.setImageResource(resource)
    }

    class Factory : PlaybackUi.Factory {
        override fun create(
            deps: SharedDependencies,
            playerController: PlayerController
        ): PlaybackUi {
            return DefaultPlaybackUi(deps, playerController)
        }
    }
}