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
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerViewWrapper
import player.common.SeekData
import player.common.requireNotNull
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.common.PlayerController
import player.ui.common.SharedDependencies
import player.ui.common.TracksState
import player.ui.common.UiState
import player.ui.sve.databinding.SvePlaybackUiBinding
import player.ui.sve.databinding.SveTabItemBinding
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// todo: move title TextView to ViewHolder?
class SvePlaybackUi(
    private val deps: SharedDependencies,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val pipController: PipController,
    private val playerController: PlayerController,
    private val playerArguments: PlayerArguments,
    private val registryOwner: SavedStateRegistryOwner,
    private val imageLoader: ImageLoader
) : PlaybackUi {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(deps.activity)
        .inflate(R.layout.sve_playback_ui, null)
    private val binding = SvePlaybackUiBinding.bind(view)
    private val seekBarListener = deps.seekBarListenerFactory.create(
        updateProgress = { position ->
            updateTimestamps(position, playerController.latestSeekData().duration)
        },
        seekTo = playerController::seekTo
    )
    private var didRestoreViewPagerState = false
    private val playerViewWrapper = playerViewWrapperFactory.create(deps.activity)
    private val adapter = SveAdapter(playerViewWrapper, imageLoader)

    init {
        registryOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                val registry = registryOwner.savedStateRegistry
                registry.registerSavedStateProvider(PROVIDER, this)
            }
        })
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
            if (!didRestoreViewPagerState) {
                didRestoreViewPagerState = true
                binding.viewPager.setCurrentItem(savedPagePosition(), false)
            }
        }
    }

    private fun savedPagePosition(default: Int = 0): Int {
        val registry = registryOwner.savedStateRegistry
        val state = registry.consumeRestoredStateForKey(PROVIDER)
        return state?.getInt(TAB_POSITION) ?: default
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
            tab.setCustomView(R.layout.sve_tab_item)
            val binding = SveTabItemBinding.bind(tab.customView.requireNotNull())
            binding.duration.text = deps.timeFormatter.playerTime(item.duration)
            binding.duration.isVisible = item.duration != Duration.ZERO
            binding.image.load(item.imageUri, imageLoader)
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(binding.tabLayout.pageChangeCallback)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val item = adapter.currentList[position]
                setTitle(item.title)
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (positionOffsetPixels == 0) {
                    playerController.toPlaylistItem(position)
                    adapter.onPageSettled(position)
                }
            }
        })

        deps.shareDelegate?.run {
            binding.share.apply {
                isVisible = true
                setOnClickListener {
                    share(deps.activity, adapter.currentList[binding.viewPager.currentItem].uri)
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

    companion object {
        private const val PROVIDER = "sve_playback_ui"
        private const val TAB_POSITION = "tab_position"
    }

    class Factory : PlaybackUi.Factory {
        override fun create(
            deps: SharedDependencies,
            playerViewWrapperFactory: PlayerViewWrapper.Factory,
            pipController: PipController,
            playerController: PlayerController,
            playerArguments: PlayerArguments,
            registryOwner: SavedStateRegistryOwner
        ): PlaybackUi {
            val imageLoader = deps.activity.applicationContext.imageLoader
            return SvePlaybackUi(deps, playerViewWrapperFactory, pipController, playerController, playerArguments, registryOwner, imageLoader)
        }
    }
}
