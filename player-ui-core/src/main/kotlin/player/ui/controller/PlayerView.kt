package player.ui.controller

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import player.common.PlayerViewWrapper
import player.common.requireNotNull
import player.ui.common.OnUserLeaveHintViewModel
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.core.R

@SuppressLint("ViewConstructor")
class PlayerView(
    context: Context,
    private val playerArguments: PlayerArguments,
    private val vmFactory: PlayerViewModel.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val errorRenderer: ErrorRenderer,
    private val pipControllerFactory: PipController.Factory,
    private val playbackUiFactories: List<PlaybackUi.Factory>,
    private val scopeFactory: () -> CoroutineScope = { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
) : CoordinatorLayout(context), LifecycleEventObserver, LifecycleOwner {
    private var scope: CoroutineScope? = null
    private val playerViewModel: PlayerViewModel by lazy {
        ViewModelProvider(
            requireViewTreeViewModelStoreOwner(),
            vmFactory.create(requireViewTreeSavedStateRegistryOwner())
        ).get(PlayerViewModel::class.java)
    }
    private val playerNonConfig: PlayerNonConfig by lazy {
        playerViewModel.get(playerArguments)
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
            activity = activity,
            playerViewWrapperFactory = playerViewWrapperFactory,
            pipController = pipController,
            playerController = playerNonConfig,
            playerArguments = playerArguments
        )
    }
    private val activity: ComponentActivity get() = context as ComponentActivity
    // Custom Lifecycle because a View can get attached/detached out of sync with its host Lifecycle.
    // This is useful for automated unregistration/cancellation of resources like back press callbacks.
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
        scope = scopeFactory()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED // CREATED is too low for back pressed dispatcher registration
        requireViewTreeLifecycleOwner().lifecycle.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.requireNotNull().cancel()
        scope = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        requireViewTreeLifecycleOwner().lifecycle.removeObserver(this)
    }

    fun release() {
        playerViewModel.remove(playerArguments.id)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            // Need to wait for View tree to be CREATED; things like the View tree's
            // SavedStateRegistry isn't available below that state.
            Lifecycle.Event.ON_CREATE -> {
                addPlaybackUi()
                setUpBackPressHandling()
                listenToPlayer()
            }
            Lifecycle.Event.ON_START -> start()
            Lifecycle.Event.ON_STOP -> stop()
        }
    }

    private fun addPlaybackUi() {
        // PlaybackUi.view will already be added to PlayerView when PlayerView is reparented.
        if (playbackUi.view.parent == this) return

        // Override default WRAP_CONTENT params when adding a child View.
        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
        addView(playbackUi.view, 0, layoutParams)
    }

    private fun start() {
        val appPlayer = playerNonConfig.getPlayer()
        playbackUi.attach(appPlayer)
    }

    private fun stop(
        forceTearDown: Boolean = false,
        // When a user backgrounds the app, then later foregrounds it back to the video, a good UX is
        // to have the player be paused upon return.
        isPlayingOverride: Boolean? = false
    ) {
        playbackUi.detachPlayer()
        val isAppBackgrounded = !activity.isChangingConfigurations
        if (isAppBackgrounded || forceTearDown) {
            playerNonConfig.tearDown(isPlayingOverride)
        }
    }

    private fun listenToPlayer() {
        val scope = scope.requireNotNull()
        playerNonConfig.playerEvents()
            .onEach { playerEvent ->
                playbackUi.onPlayerEvent(playerEvent)
                pipController.onEvent(playerEvent)
            }
            .launchIn(scope)

        playerNonConfig.uiStates()
            .onEach { uiState -> playbackUi.onUiState(uiState) }
            .launchIn(scope)

        playerNonConfig.tracksStates()
            .onEach { tracksState -> playbackUi.onTracksState(tracksState) }
            .launchIn(scope)

        playerNonConfig.errors()
            .onEach { message ->
                val action = ErrorRenderer.Action(
                    name = activity.getString(R.string.retry),
                    callback = {
                        stop(forceTearDown = true, isPlayingOverride = true) // Enter playing state if retry was successful
                        start()
                    }
                )
                errorRenderer.render(this, message, action)
            }
            .launchIn(scope)

        playerNonConfig.playbackInfos
            .onEach { playbackInfos -> playbackUi.onPlaybackInfos(playbackInfos) }
            .launchIn(scope)

        if (playerArguments.pipConfig?.isEnabled == true) {
            pipController.events()
                .flowWithLifecycle(requireViewTreeLifecycleOwner().lifecycle)
                .launchIn(scope)

            if (playerArguments.pipConfig?.onUserLeaveHints == true) {
                onUserLeaveHintViewModel.onUserLeaveHints()
                    .onEach { enterPip() }
                    .launchIn(scope)
            }
        }
    }

    private fun enterPip(): PipController.Result {
        return pipController.enterPip()
    }

    private fun setUpBackPressHandling() {
        val pipConfig = playerArguments.pipConfig
        val pipOnBackPress = pipConfig?.onBackPresses == true
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
