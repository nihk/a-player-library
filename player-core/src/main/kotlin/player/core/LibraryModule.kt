package player.core

import androidx.fragment.app.FragmentActivity
import player.common.PlayerModule
import player.ui.common.PipController
import player.ui.common.isMinOsForPip
import player.ui.controller.AndroidPipController
import player.ui.controller.NoOpPipController
import player.ui.controller.PlayerNonConfig
import player.ui.controller.PlayerView
import player.ui.controller.PlayerViewModel
import player.ui.controller.SnackbarErrorRenderer

internal class LibraryModule(activity: FragmentActivity) {
    private val libraryConfiguration = (activity as? LibraryConfiguration.Provider
        ?: LibraryInitializer).libraryConfiguration

    val playerViewFactory: PlayerView.Factory get() = PlayerView.Factory(
        vmFactory = playerViewModelFactory,
        playerViewWrapperFactory = module.playerViewWrapperFactory,
        errorRenderer = SnackbarErrorRenderer(),
        pipControllerFactory = pipControllerFactory,
        playbackUiFactories = libraryConfiguration.playbackUiFactories
    )

    private val module: PlayerModule = libraryConfiguration.playerModule

    private val pipControllerFactory: PipController.Factory = if (isMinOsForPip) {
        AndroidPipController.Factory(activity)
    } else {
        NoOpPipController.Factory()
    }

    private val playerNonConfigFactory: PlayerNonConfig.Factory get() = PlayerNonConfig.Factory(
        appPlayerFactory = module.appPlayerFactory,
        playerEventStream = module.playerEventStream,
        playerEventDelegate = libraryConfiguration.playerEventDelegate,
        playbackInfoResolver = libraryConfiguration.playbackInfoResolver,
        seekDataUpdater = module.seekDataUpdater
    )

    private val playerViewModelFactory: PlayerViewModel.Factory get() = PlayerViewModel.Factory(
       playerNonConfigFactory
    )
}
