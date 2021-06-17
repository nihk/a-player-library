package player.core

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import player.common.PlayerModule
import player.ui.common.Navigator
import player.ui.common.PipController
import player.ui.common.SharedDependencies
import player.ui.common.isMinOsForPip
import player.ui.controller.AndroidPipController
import player.ui.controller.DefaultSeekBarListener
import player.ui.controller.NoOpPipController
import player.ui.controller.PlayerFragment
import player.ui.controller.PlayerViewModel
import player.ui.controller.SnackbarErrorRenderer
import player.ui.controller.TracksPickerFragment

internal class LibraryModule(
    private val activity: FragmentActivity,
    private val fragmentManager: FragmentManager
) {
    val fragmentFactory: FragmentFactory get() = LibraryFragmentFactory(fragmentMap)

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
                deps = SharedDependencies(
                    context = activity,
                    shareDelegate = LibraryInitializer.shareDelegate(),
                    seekBarListenerFactory = DefaultSeekBarListener.Factory(),
                    timeFormatter = LibraryInitializer.timeFormatter(),
                    navigator = navigator,
                ),
                pipControllerFactory = pipControllerFactory
            )
        },
        TracksPickerFragment::class.java to { TracksPickerFragment() }
    )

    private val playerViewModelFactory: PlayerViewModel.Factory get() = PlayerViewModel.Factory(
        appPlayerFactory = module.appPlayerFactory,
        playerEventStream = module.playerEventStream,
        telemetry = LibraryInitializer.telemetry(),
        playbackInfoResolver = LibraryInitializer.playbackInfoResolver(),
        seekDataUpdater = module.seekDataUpdater
    )

    private val navigator: Navigator
        get() = LibraryNavigator(fragmentManager, R.id.container)
}
