package player.ui.controller

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import player.common.PlayerViewWrapper
import player.ui.common.OnUserLeaveHintViewModel
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import java.util.*

// todo: embed this in PlayerFragment (or LibraryView in LibraryFragment)?
@SuppressLint("ViewConstructor")
class PlayerView(
    context: Context,
    private val playerArguments: PlayerArguments,
    private val uuid: UUID,
    private val vmFactory: PlayerViewModel2.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val errorRenderer: ErrorRenderer,
    private val pipControllerFactory: PipController.Factory,
    private val playbackUiFactories: List<PlaybackUi.Factory>
) : FrameLayout(context), LifecycleEventObserver, LifecycleOwner {
    private val playerViewModel2: PlayerViewModel2 by lazy {
        ViewModelProvider(
            requireViewTreeViewModelStoreOwner(),
            vmFactory.create(requireViewTreeSaveStateRegistryOwner())
        ).get(PlayerViewModel2::class.java)
    }
    private val playerNonConfig: PlayerNonConfig by lazy {
        playerViewModel2.get(uuid, playerArguments.uri)
    }
    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by lazy {
        ViewModelProvider(activity).get(OnUserLeaveHintViewModel::class.java)
    }
    private val pipController: PipController by lazy { pipControllerFactory.create(playerNonConfig) }
    private val playbackUi: PlaybackUi by lazy {
        playbackUiFactories.first { factory ->
            playerArguments.playbackUiFactory.isAssignableFrom(factory::class.java)
        }.create(
            host = context as FragmentActivity,
            playerViewWrapperFactory = playerViewWrapperFactory,
            pipController = pipController,
            playerController = playerNonConfig,
            playerArguments = playerArguments,
            registryOwner = requireViewTreeSaveStateRegistryOwner()
        )
    }
    private val activity: ComponentActivity get() = context as ComponentActivity
    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        id = View.generateViewId()
        keepScreenOn = true
        isClickable = true
        isFocusable = true
        background = ColorDrawable(Color.BLACK)

        doOnAttach {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED // Created is too low for back pressed dispatcher
            addView(playbackUi.view)
            setUpBackPressHandling()
            listenToPlayer()
            activity.lifecycle.addObserver(this)

            // Nested because otherwise it will be called immediately, before View is attached.
            doOnDetach {
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                val isPlayerClosed = !activity.isChangingConfigurations
                if (isPlayerClosed) {
                    playerViewModel2.remove(uuid)
                    activity.lifecycle.removeObserver(this)
                }
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                val appPlayer = playerNonConfig.getPlayer()
                playbackUi.attach(appPlayer)
            }
            Lifecycle.Event.ON_STOP -> {
                playbackUi.detachPlayer()
                val isAppBackgrounded = !activity.isChangingConfigurations
                if (isAppBackgrounded) {
                    playerNonConfig.onAppBackgrounded()
                }
            }
        }
    }

    private fun listenToPlayer() {
        playerNonConfig.playerEvents()
            .onEach { playerEvent ->
                playbackUi.onPlayerEvent(playerEvent)
                pipController.onEvent(playerEvent)
            }
            .launchIn(lifecycleScope)

        playerNonConfig.uiStates()
            .onEach { uiState -> playbackUi.onUiState(uiState) }
            .launchIn(lifecycleScope)

        playerNonConfig.tracksStates()
            .onEach { tracksState -> playbackUi.onTracksState(tracksState) }
            .launchIn(lifecycleScope)

        playerNonConfig.errors()
            .onEach { message -> errorRenderer.render(this, message) }
            .launchIn(lifecycleScope)

        playerNonConfig.playbackInfos
            .onEach { playbackInfos -> playbackUi.onPlaybackInfos(playbackInfos) }
            .launchIn(lifecycleScope)

        if (playerArguments.pipConfig?.enabled == true) {
            pipController.events()
                .launchIn(lifecycleScope)

            onUserLeaveHintViewModel.onUserLeaveHints()
                .onEach { enterPip() }
                .launchIn(lifecycleScope)
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
        activity.onBackPressedDispatcher.addCallback(this, onBackPressed)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    class Factory(
        private val vmFactory: PlayerViewModel2.Factory,
        private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
        private val errorRenderer: ErrorRenderer,
        private val pipControllerFactory: PipController.Factory,
        private val playbackUiFactories: List<PlaybackUi.Factory>,
    ) {
        fun create(
            context: Context,
            playerArguments: PlayerArguments,
            uuid: UUID
        ): PlayerView {
            return PlayerView(
                context = context,
                playerArguments = playerArguments,
                uuid = uuid,
                vmFactory = vmFactory,
                playerViewWrapperFactory = playerViewWrapperFactory,
                errorRenderer = errorRenderer,
                pipControllerFactory = pipControllerFactory,
                playbackUiFactories = playbackUiFactories
            )
        }
    }
}
