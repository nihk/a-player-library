package player.ui.controller

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnAttach
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import player.common.PlayerViewWrapper
import player.ui.common.OnUserLeaveHintViewModel
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments

@SuppressLint("ViewConstructor")
class PlayerView(
    context: Context,
    private val playerArguments: PlayerArguments,
    private val vmFactory: PlayerViewModel.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val errorRenderer: ErrorRenderer,
    private val pipControllerFactory: PipController.Factory,
    private val playbackUiFactories: List<PlaybackUi.Factory>
) : FrameLayout(context), LifecycleEventObserver {
    private val playerViewModel: PlayerViewModel by lazy {
        ViewModelProvider(
            requireViewTreeViewModelStoreOwner(),
            vmFactory.create(requireViewTreeSaveStateRegistryOwner(), playerArguments.uri)
        ).get(PlayerViewModel::class.java)
    }
    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by lazy {
        ViewModelProvider(activity)
            .get(OnUserLeaveHintViewModel::class.java)
    }
    private val pipController: PipController by lazy { pipControllerFactory.create(playerViewModel) }
    private val playbackUi: PlaybackUi by lazy {
        playbackUiFactories.first { factory ->
            playerArguments.playbackUiFactory.isAssignableFrom(factory::class.java)
        }.create(
            host = context as FragmentActivity,
            playerViewWrapperFactory = playerViewWrapperFactory,
            pipController = pipController,
            playerController = playerViewModel,
            playerArguments = playerArguments,
            registryOwner = requireViewTreeSaveStateRegistryOwner()
        )
    }
    private val activity: ComponentActivity get() = context as ComponentActivity

    init {
        keepScreenOn = true
        isClickable = true
        isFocusable = true
        background = ColorDrawable(Color.BLACK)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        doOnAttach {
            addView(playbackUi.view)
            setUpBackPressHandling()
            listenToPlayer()
            activity.lifecycle.addObserver(this)
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                val appPlayer = playerViewModel.getPlayer()
                playbackUi.attach(appPlayer)
            }
            Lifecycle.Event.ON_STOP -> {
                playbackUi.detachPlayer()
                if (!activity.isChangingConfigurations) {
                    playerViewModel.onAppBackgrounded()
                }
            }
        }
    }

    private fun listenToPlayer() {
        playerViewModel.playerEvents()
            .onEach { playerEvent ->
                playbackUi.onPlayerEvent(playerEvent)
                pipController.onEvent(playerEvent)
            }
            .launchIn(requireViewTreeLifecycleOwner().lifecycleScope)

        playerViewModel.uiStates()
            .onEach { uiState -> playbackUi.onUiState(uiState) }
            .launchIn(requireViewTreeLifecycleOwner().lifecycleScope)

        playerViewModel.tracksStates()
            .onEach { tracksState -> playbackUi.onTracksState(tracksState) }
            .launchIn(requireViewTreeLifecycleOwner().lifecycleScope)

        playerViewModel.errors()
            .onEach { message -> errorRenderer.render(this, message) }
            .launchIn(requireViewTreeLifecycleOwner().lifecycleScope)

        playerViewModel.playbackInfos
            .onEach { playbackInfos -> playbackUi.onPlaybackInfos(playbackInfos) }
            .launchIn(requireViewTreeLifecycleOwner().lifecycleScope)

        if (playerArguments.pipConfig?.enabled == true) {
            pipController.events()
                .launchIn(requireViewTreeLifecycleOwner().lifecycleScope)

            onUserLeaveHintViewModel.onUserLeaveHints()
                .onEach { enterPip() }
                .launchIn(requireViewTreeLifecycleOwner().lifecycleScope)
        }
    }

    private fun enterPip(): PipController.Result {
        return pipController.enterPip()
    }

    private fun setUpBackPressHandling() {
        val pipConfig = playerArguments.pipConfig
        val pipOnBackPress = pipConfig?.enabled == true && pipConfig.onBackPresses
        if (!pipOnBackPress) return
        val activity = context as ComponentActivity

        val onBackPressed = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val result = enterPip()
                if (result == PipController.Result.DidNotEnterPip) {
                    remove()
                    activity.onBackPressed()
                }
            }
        }
        activity.onBackPressedDispatcher.addCallback(requireViewTreeLifecycleOwner(), onBackPressed)
    }

    class Factory(
        private val vmFactory: PlayerViewModel.Factory,
        private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
        private val errorRenderer: ErrorRenderer,
        private val pipControllerFactory: PipController.Factory,
        private val playbackUiFactories: List<PlaybackUi.Factory>,
    ) {
        fun create(
            context: Context,
            playerArguments: PlayerArguments
        ): PlayerView {
            return PlayerView(
                context = context,
                playerArguments = playerArguments,
                vmFactory = vmFactory,
                playerViewWrapperFactory = playerViewWrapperFactory,
                errorRenderer = errorRenderer,
                pipControllerFactory = pipControllerFactory,
                playbackUiFactories = playbackUiFactories
            )
        }
    }
}
