package player.ui.controller

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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

// todo: PiP UI states
@SuppressLint("ViewConstructor")
class PlayerView(
    context: Context,
    private val playerArguments: PlayerArguments,
    private val keyPlayerNonConfig: UUID,
    private val vmFactory: PlayerViewModel.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val errorRenderer: ErrorRenderer,
    private val pipControllerFactory: PipController.Factory,
    private val playbackUiFactories: List<PlaybackUi.Factory>
) : FrameLayout(context), LifecycleEventObserver, LifecycleOwner {
    private val playerViewModel: PlayerViewModel by lazy {
        ViewModelProvider(
            requireViewTreeViewModelStoreOwner(),
            vmFactory.create(requireViewTreeSavedStateRegistryOwner())
        ).get(PlayerViewModel::class.java)
    }
    private val playerNonConfig: PlayerNonConfig by lazy {
        playerViewModel.get(keyPlayerNonConfig, playerArguments.uri)
    }
    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by lazy {
        // Activity scoped because Activity.onUserLeaveHint is only available at the Activity level.
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
            registryOwner = activity
        )
    }
    private val activity: ComponentActivity get() = context as ComponentActivity
    // Custom Lifecycle because a View can get attached/detach out of sync with its host Lifecycle.
    // This is useful for automated unregistration/cancellation of resources like back press callbacks,
    // coroutine scopes.
    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        id = View.generateViewId()
        keepScreenOn = true
        isClickable = true
        isFocusable = true
        background = ColorDrawable(Color.BLACK)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED // CREATED is too low for back pressed dispatcher registration
        requireViewTreeLifecycleOwner().lifecycle.addObserver(this)
    }

    // note: this assumes that this View being detached from the Window means a destructive action.
    // Reparenting this View won't behave as expected in this current state, because reparenting
    // detaches the View from the Window.
    // Also, once a lifecycle state is DESTROYED, its lifecycleScope is cancelled and cannot be reused.
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        requireViewTreeLifecycleOwner().lifecycle.removeObserver(this)

        val isPlayerClosed = !activity.isChangingConfigurations
        if (isPlayerClosed) {
            playerViewModel.remove(keyPlayerNonConfig)
        } // else keep PlayerNonConfig around in PlayerViewModel to be used when state is restored after config change
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            // Need to wait for View tree to be CREATED; things like the View tree's
            // SavedStateRegistry isn't available below that state.
            Lifecycle.Event.ON_CREATE -> {
                addView(playbackUi.view)
                setUpBackPressHandling()
                listenToPlayer()
            }
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
        private val vmFactory: PlayerViewModel.Factory,
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
                keyPlayerNonConfig = uuid,
                vmFactory = vmFactory,
                playerViewWrapperFactory = playerViewWrapperFactory,
                errorRenderer = errorRenderer,
                pipControllerFactory = pipControllerFactory,
                playbackUiFactories = playbackUiFactories
            )
        }
    }
}
