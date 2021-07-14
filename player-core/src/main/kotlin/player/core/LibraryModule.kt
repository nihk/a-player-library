package player.core

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import player.common.PlayerModule
import player.ui.common.PipController
import player.ui.common.isMinOsForPip
import player.ui.controller.AndroidPipController
import player.ui.controller.NoOpPipController
import player.ui.controller.PlayerFragment
import player.ui.controller.PlayerNonConfig
import player.ui.controller.PlayerView
import player.ui.controller.PlayerViewModel
import player.ui.controller.PlayerViewModel2
import player.ui.controller.SnackbarErrorRenderer

internal class LibraryModule(activity: FragmentActivity) {
    val fragmentFactory: FragmentFactory get() = LibraryFragmentFactory(fragmentMap)
    val playerViewFactory: PlayerView.Factory get() = PlayerView.Factory(
        vmFactory = playerViewModel2Factory,
        playerViewWrapperFactory = module.playerViewWrapperFactory,
        errorRenderer = SnackbarErrorRenderer(),
        pipControllerFactory = pipControllerFactory,
        playbackUiFactories = LibraryInitializer.playbackUiFactories()
    )

    private val module: PlayerModule = LibraryInitializer.playerModule()

    private val pipControllerFactory: PipController.Factory = if (isMinOsForPip) {
        AndroidPipController.Factory(activity)
    } else {
        NoOpPipController.Factory()
    }

    private val fragmentMap: Map<Class<out Fragment>, () -> Fragment> get() = mapOf(
        PlayerFragment::class.java to {
            PlayerFragment(
                vmFactory = playerViewModelFactory,
                playerViewWrapperFactory = module.playerViewWrapperFactory,
                errorRenderer = SnackbarErrorRenderer(),
                pipControllerFactory = pipControllerFactory,
                playbackUiFactories = LibraryInitializer.playbackUiFactories()
            )
        },
    ) + LibraryInitializer.playbackUiFactories().fold(emptyMap<Class<out Fragment>, () -> Fragment>()) { accumulator, factory ->
        accumulator + factory.fragmentMap
    }

    private val playerNonConfigFactory: PlayerNonConfig.Factory get() = PlayerNonConfig.Factory(
        appPlayerFactory = module.appPlayerFactory,
        playerEventStream = module.playerEventStream,
        playerEventDelegate = LibraryInitializer.playerEventDelegate(),
        playbackInfoResolver = LibraryInitializer.playbackInfoResolver(),
        seekDataUpdater = module.seekDataUpdater
    )

    private val playerViewModel2Factory: PlayerViewModel2.Factory get() = PlayerViewModel2.Factory(
       playerNonConfigFactory
    )

    private val playerViewModelFactory: PlayerViewModel.Factory get() = PlayerViewModel.Factory(
        appPlayerFactory = module.appPlayerFactory,
        playerEventStream = module.playerEventStream,
        playerEventDelegate = LibraryInitializer.playerEventDelegate(),
        playbackInfoResolver = LibraryInitializer.playbackInfoResolver(),
        seekDataUpdater = module.seekDataUpdater
    )
}
