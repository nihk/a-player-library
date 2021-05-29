package library.core

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import library.common.PlayerModule
import library.common.isMinOsForPip
import library.ui.AndroidPipController
import library.ui.Navigator
import library.ui.NoOpPipController
import library.ui.PlayerFragment
import library.ui.PlayerViewModel
import library.ui.SnackbarErrorRenderer
import library.ui.TracksPickerFragment

internal class LibraryModule(private val fragment: Fragment) {
    val fragmentFactory: FragmentFactory get() = LibraryFragmentFactory(fragmentMap)

    private val module: PlayerModule = LibraryInitializer.playerModule()

    private val fragmentMap: Map<Class<out Fragment>, () -> Fragment> get() = mapOf(
        PlayerFragment::class.java to {
            PlayerFragment(
                vmFactory = playerViewModelFactory,
                playerViewWrapperFactory = module.playerViewWrapperFactory,
                shareDelegate = LibraryInitializer.shareDelegate(),
                pipController = if (isMinOsForPip) {
                    AndroidPipController(fragment.requireActivity())
                } else {
                    NoOpPipController()
                },
                errorRenderer = SnackbarErrorRenderer(),
                navigator = navigator,
                timeFormatter = LibraryInitializer.timeFormatter()
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

    private val navigator: Navigator get() = LibraryNavigator(
        fragment.childFragmentManager,
        R.id.container
    )
}
