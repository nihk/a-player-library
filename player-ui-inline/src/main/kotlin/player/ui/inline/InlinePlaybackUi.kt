package player.ui.inline

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerViewWrapper
import player.common.SeekData
import player.common.VideoSize
import player.ui.common.CloseDelegate
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.common.PlayerController
import player.ui.common.TracksState
import player.ui.common.UiState
import player.ui.controller.requireViewTreeLifecycleOwner
import player.ui.inline.databinding.InlinePlaybackUiBinding

// todo: a dedicated fullscreen/smallscreen UI (depending on state)
class InlinePlaybackUi(
    private val activity: ComponentActivity,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val pipController: PipController,
    private val playerController: PlayerController,
    private val playerArguments: PlayerArguments,
    private val closeDelegate: CloseDelegate,
    private val onFullscreenChangedCallback: OnFullscreenChangedCallback,
    private val onVideoSizeChangedCallback: OnVideoSizeChangedCallback?,
    private val isFullscreenInitially: Boolean? = null
) : PlaybackUi {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(activity)
        .inflate(R.layout.inline_playback_ui, null)
    private val binding = InlinePlaybackUiBinding.bind(view)

    private val playerViewWrapper = playerViewWrapperFactory.create(activity)
    private val backPress: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            closeFullscreen()
        }
    }

    init {
        view.doOnAttach {
            binding.playerContainer.addView(playerViewWrapper.view)
            bindControls()
        }
    }

    private fun bindControls() {
        if (isFullscreenInitially != null) {
            setFullscreen(isFullscreenInitially)
        }
        binding.fullscreen.setOnClickListener {
            setFullscreen(isFullscreen = true)
            onFullscreenChangedCallback.onFullscreenChanged(isFullscreen = true, activity)
        }
        binding.closeFullscreen.setOnClickListener {
            closeFullscreen()
        }

        setPlayPause(playerController.isPlaying())
        binding.playPause.setOnClickListener { view ->
            if (view.isSelected) {
                playerController.pause()
            } else {
                playerController.play()
            }
        }

        binding.close.setOnClickListener {
            closeDelegate.onClose(activity)
        }
    }

    private fun closeFullscreen() {
        setFullscreen(isFullscreen = false)
        onFullscreenChangedCallback.onFullscreenChanged(isFullscreen = false, activity)
    }

    private fun setFullscreen(isFullscreen: Boolean) {
        binding.closeFullscreen.isVisible = isFullscreen
        binding.fullscreen.isVisible = !isFullscreen
        if (isFullscreen) {
            addBackPress()
        } else {
            backPress.remove()
        }
    }

    private fun setPlayPause(isPlaying: Boolean) {
        binding.playPause.isSelected = isPlaying
        val a11y = if (isPlaying) R.string.pause else R.string.play
        binding.playPause.contentDescription = activity.getString(a11y)
    }

    override fun onPlayerEvent(playerEvent: PlayerEvent) {
        playerViewWrapper.onEvent(playerEvent)
        when (playerEvent) {
            is PlayerEvent.Initial -> setPlayPause(playerController.isPlaying())
            is PlayerEvent.OnIsPlayingChanged -> setPlayPause(playerEvent.isPlaying)
            is PlayerEvent.OnVideoSizeChanged -> {
                val videoSize = VideoSize(
                    widthPx = playerEvent.width,
                    heightPx = playerEvent.height
                )
                onVideoSizeChangedCallback?.onVideoSizeChanged(videoSize, activity)
            }
        }
    }

    override fun onUiState(uiState: UiState) = Unit
    override fun onSeekData(seekData: SeekData) = Unit
    override fun onTracksState(tracksState: TracksState) = Unit
    override fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>) = Unit

    override fun attach(appPlayer: AppPlayer) {
        playerViewWrapper.attach(appPlayer)
    }

    override fun detachPlayer() {
        playerViewWrapper.detachPlayer()
    }

    private fun addBackPress() {
        activity.onBackPressedDispatcher.addCallback(view.requireViewTreeLifecycleOwner(), backPress)
    }

    class Factory(
        private val closeDelegate: CloseDelegate,
        private val onFullscreenChangedCallback: OnFullscreenChangedCallback,
        private val onVideoSizeChangedCallback: OnVideoSizeChangedCallback? = null,
        private val isFullscreenInitially: Boolean? = null
    ) : PlaybackUi.Factory {
        override fun create(
            activity: ComponentActivity,
            playerViewWrapperFactory: PlayerViewWrapper.Factory,
            pipController: PipController,
            playerController: PlayerController,
            playerArguments: PlayerArguments,
        ): PlaybackUi {
            return InlinePlaybackUi(
                activity = activity,
                playerViewWrapperFactory = playerViewWrapperFactory,
                pipController = pipController,
                playerController = playerController,
                playerArguments = playerArguments,
                closeDelegate = closeDelegate,
                onFullscreenChangedCallback = onFullscreenChangedCallback,
                onVideoSizeChangedCallback = onVideoSizeChangedCallback,
                isFullscreenInitially = isFullscreenInitially
            )
        }
    }
}
