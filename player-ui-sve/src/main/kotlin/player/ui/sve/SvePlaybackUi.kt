package player.ui.sve

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.SavedStateRegistryOwner
import androidx.viewpager2.widget.ViewPager2
import coil.ImageLoader
import coil.imageLoader
import coil.load
import com.google.android.material.tabs.TabLayoutMediator
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.SeekData
import player.common.requireNotNull
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.common.PlayerController
import player.ui.common.SharedDependencies
import player.ui.common.TracksState
import player.ui.common.UiState
import player.ui.sve.databinding.SveItemBinding
import player.ui.sve.databinding.SvePlaybackUiBinding
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SvePlaybackUi(
    private val deps: SharedDependencies,
    private val pipController: PipController,
    private val playerController: PlayerController,
    private val playerArguments: PlayerArguments,
    private val registryOwner: SavedStateRegistryOwner,
    private val imageLoader: ImageLoader
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
        registryOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                val registry = registryOwner.savedStateRegistry
                registry.registerSavedStateProvider(PROVIDER, this)
            }
        })
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
    }

    override fun onTracksState(tracksState: TracksState) = Unit

    override fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>) {
        val titles = playbackInfos.filterIsInstance<PlaybackInfo.MediaTitle>()
            .associate { it.mediaUriRef to it.title }

        val mainUri = playbackInfos.filterIsInstance<PlaybackInfo.MediaUri>()
            .map { mediaUri ->
                SveItem(
                    title = titles[mediaUri.uri],
                    uri = mediaUri.uri,
                    imageUri = "",
                    duration = Duration.ZERO
                )
            }
        val relatedMedia = playbackInfos.filterIsInstance<PlaybackInfo.RelatedMedia>()
            .map { relatedMedia ->
                SveItem(
                    title = titles[relatedMedia.uri],
                    uri = relatedMedia.uri,
                    imageUri = relatedMedia.imageUri,
                    duration = relatedMedia.durationMillis.toDuration(DurationUnit.MILLISECONDS)
                )
            }
        val toSubmit = mainUri + relatedMedia
        if (toSubmit.isNotEmpty()) {
            val item = toSubmit[binding.viewPager.currentItem]
            setTitle(item.title)

            adapter.submitList(toSubmit)
            restoreSelectedPageState()
        }
    }

    private fun setTitle(title: String?) {
        binding.title.apply {
            isVisible = title != null
            text = title
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
            val item = adapter.currentList[position]
            tab.setCustomView(R.layout.sve_item)
            val binding = SveItemBinding.bind(tab.customView.requireNotNull())
            binding.duration.text = deps.timeFormatter.playerTime(item.duration)
            binding.duration.isVisible = item.duration != Duration.ZERO
            binding.image.load(item.imageUri, imageLoader)
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(binding.tabLayout.pageChangeCallback)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var ignoreInitialTrigger = true
            override fun onPageSelected(position: Int) {
                val item = adapter.currentList[position]
                setTitle(item.title)

                if (ignoreInitialTrigger) {
                    ignoreInitialTrigger = false
                    return
                }
                playerController.toPlaylistItem(position)
            }
        })

        deps.shareDelegate?.run {
            binding.share.apply {
                isVisible = true
                setOnClickListener {
                    share(deps.context, adapter.currentList[binding.viewPager.currentItem].uri)
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

    override fun saveState(): Bundle {
        return bundleOf(TAB_POSITION to binding.tabLayout.selectedTabPosition)
    }

    private fun restoreSelectedPageState() {
        val registry = registryOwner.savedStateRegistry
        val state = registry.consumeRestoredStateForKey(PROVIDER) ?: return
        val position = state.getInt(TAB_POSITION)
        binding.viewPager.setCurrentItem(position, false)
    }

    companion object {
        private const val PROVIDER = "sve_playback_ui"
        private const val TAB_POSITION = "tab_position"
    }

    class Factory : PlaybackUi.Factory {
        override fun create(
            deps: SharedDependencies,
            pipController: PipController,
            playerController: PlayerController,
            playerArguments: PlayerArguments,
            registryOwner: SavedStateRegistryOwner
        ): PlaybackUi {
            val imageLoader = deps.context.applicationContext.imageLoader
            return SvePlaybackUi(deps, pipController, playerController, playerArguments, registryOwner, imageLoader)
        }
    }
}
