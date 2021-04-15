package library

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import library.common.PlayerModule
import library.ui.PlayerFragment
import library.ui.PlayerViewModel
import library.ui.TracksPickerFragment

internal class LibraryModule {
    val fragmentFactory: FragmentFactory get() = LibraryFragmentFactory(fragmentMap)

    private val module: PlayerModule = LibraryInitializer.playerModule()
    private val fragmentMap: Map<Class<out Fragment>, () -> Fragment> get() = mapOf(
        PlayerFragment::class.java to {
            PlayerFragment(
                vmFactory = playerViewModelFactory,
                playerViewWrapperFactory = module.playerViewWrapperFactory,
                shareDelegate = LibraryInitializer.shareDelegate()
            )
        },
        TracksPickerFragment::class.java to { TracksPickerFragment() }
    )
    private val playerViewModelFactory: PlayerViewModel.Factory get() = PlayerViewModel.Factory(
        appPlayerFactory = module.appPlayerFactory,
        playerEventStream = module.playerEventStream,
        telemetry = LibraryInitializer.telemetry()
    )
}