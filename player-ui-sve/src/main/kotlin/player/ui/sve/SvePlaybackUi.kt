package player.ui.sve

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import coil.ImageLoader
import coil.imageLoader
import com.google.android.material.tabs.TabLayoutMediator
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.SeekData
import player.common.requireNotNull
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.common.PlayerController
import player.ui.common.SharedDependencies
import player.ui.common.TracksState
import player.ui.common.UiState
import player.ui.sve.databinding.SvePlaybackUiBinding
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SvePlaybackUi(
    private val deps: SharedDependencies,
    private val playerController: PlayerController,
    private val playerArguments: PlayerArguments,
    imageLoader: ImageLoader
) : PlaybackUi {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(deps.context)
        .inflate(R.layout.sve_playback_ui, null)
    private val binding = SvePlaybackUiBinding.bind(view)
    private val seekBarListener = deps.seekBarListenerFactory.create(
        updateProgress = { position ->
            updateTimestamps(position, playerController.latestSeekData().duration)
        },
        seekTo = playerController::seekTo
    )
    private val adapter = SveAdapter()

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
    }

    override fun onTracksState(tracksState: TracksState) = Unit

    override fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>) {
        playbackInfos.forEach { playbackInfo ->
            when (playbackInfo) {
                is PlaybackInfo.MediaTitle -> {
                    binding.title.apply {
                        isVisible = true
                        text = playbackInfo.title
                    }
                }
                is PlaybackInfo.RelatedMedia -> {
                    val sveItems = playbackInfo.metadata.map { metadata ->
                        SveItem(
                            uri = metadata.uri,
                            imageUri = metadata.imageUri,
                            duration = metadata.durationMillis.toDuration(DurationUnit.MILLISECONDS)
                        )
                    }
                    adapter.submitList(sveItems)
                }
            }
        }
    }

    private fun SeekBar.update(seekData: SeekData) {
        max = seekData.duration.inWholeSeconds.toInt()
        progress = seekData.position.inWholeSeconds.toInt()
        secondaryProgress = seekData.buffered.inWholeSeconds.toInt()
    }

    private fun bindControls() {
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.currentList[position].uri
        }.attach()
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // fixme: config change is calling this
                val uri = adapter.currentList[position].uri
                playerController.toPlaylistItem(uri)
            }
        })

        deps.shareDelegate?.run {
            binding.share.apply {
                isVisible = true
                setOnClickListener {
                    // fixme: this needs to be the currently selected
                    share(deps.context, playerArguments.uri)
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
            playerController: PlayerController,
            playerArguments: PlayerArguments
        ): PlaybackUi {
            val imageLoader = deps.context.applicationContext.imageLoader
            return SvePlaybackUi(deps, playerController, playerArguments, imageLoader)
        }
    }
}
