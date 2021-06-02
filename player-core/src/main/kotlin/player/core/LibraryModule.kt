package player.core

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import player.common.PlayerModule
import player.ui.controller.PlayerFragment
import player.ui.shared.AndroidPipController
import player.ui.shared.DefaultSeekBarListener
import player.ui.shared.Navigator
import player.ui.shared.NoOpPipController
import player.ui.shared.PipController
import player.ui.controller.SnackbarErrorRenderer
import player.ui.controller.TracksPickerFragment
import player.ui.shared.PlayerArguments
import player.ui.shared.PlayerViewModel
import player.ui.shared.SharedDependencies
import player.ui.shared.isMinOsForPip

internal class LibraryModule(
    private val activity: FragmentActivity,
    private val fragmentManager: FragmentManager
) {
    val fragmentFactory: FragmentFactory get() = LibraryFragmentFactory(fragmentMap)

    private val module: PlayerModule = LibraryInitializer.playerModule()

    private val pipController: PipController = if (isMinOsForPip) {
        AndroidPipController(activity)
    } else {
        NoOpPipController()
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
                    pipController = pipController,
                    navigator = navigator,
                )
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
