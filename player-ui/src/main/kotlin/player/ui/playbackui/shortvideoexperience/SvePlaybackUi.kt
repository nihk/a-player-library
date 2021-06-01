package player.ui.playbackui.shortvideoexperience

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import coil.ImageLoader
import player.common.PlayerArguments
import player.common.PlayerEvent
import player.common.SeekData
import player.common.ShareDelegate
import player.common.TimeFormatter
import player.common.requireNotNull
import player.ui.Navigator
import player.ui.PipController
import player.ui.PlayerController
import player.ui.R
import player.ui.SeekBarListener
import player.ui.TracksState
import player.ui.UiState
import player.ui.databinding.SvePlaybackUiBinding
import player.ui.playbackui.PlaybackUi
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SvePlaybackUi(
    private val playerArguments: PlayerArguments,
    private val playerController: PlayerController,
    private val activity: FragmentActivity,
    private val shareDelegate: ShareDelegate?,
    seekBarListenerFactory: SeekBarListener.Factory,
    private val timeFormatter: TimeFormatter,
    private val pipController: PipController,
    private val navigator: Navigator,
    private val imageLoader: ImageLoader
) : PlaybackUi {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(activity)
        .inflate(R.layout.sve_playback_ui, null)

    private val binding = SvePlaybackUiBinding.bind(view)

    private val seekBarListener = seekBarListenerFactory.create(
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
        binding.root.isVisible = uiState.isControllerUsable && !pipController.isInPip()
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

    override fun onTracksState(tracksState: TracksState) = Unit

    private fun SeekBar.update(seekData: SeekData) {
        max = seekData.duration.inWholeSeconds.toInt()
        progress = seekData.position.inWholeSeconds.toInt()
        secondaryProgress = seekData.buffered.inWholeSeconds.toInt()
    }

    private fun bindControls() {
        val adapter = SveAdapter(imageLoader, navigator, playerArguments, timeFormatter)
        val sveItems = playerArguments.links.map { link ->
            SveItem(
                uri = link.uri,
                imageUri = link.imageUri,
                duration = link.durationMillis.toDuration(DurationUnit.MILLISECONDS)
            )
        }
        binding.recyclerView.adapter = adapter.also { it.submitList(sveItems) }

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
        binding.playPause.setOnClickListener {
            if (playerController.isPlaying()) {
                playerController.pause()
            } else {
                playerController.play()
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
        val resource = if (isPlaying) {
            R.drawable.pause
        } else {
            R.drawable.play
        }
        binding.playPause.setImageResource(resource)
    }
}
