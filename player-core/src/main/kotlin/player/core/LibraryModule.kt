package player.core

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import coil.imageLoader
import player.common.PlayerArguments
import player.common.PlayerModule
import player.common.isMinOsForPip
import player.ui.AndroidPipController
import player.ui.playbackui.DefaultPlaybackUiFactory
import player.ui.DefaultSeekBarListener
import player.ui.Navigator
import player.ui.NoOpPipController
import player.ui.PipController
import player.ui.PlayerFragment
import player.ui.PlayerViewModel
import player.ui.SnackbarErrorRenderer
import player.ui.TracksPickerFragment

internal class LibraryModule(
    private val fragment: Fragment,
    private val playerArguments: PlayerArguments
) {
    val fragmentFactory: FragmentFactory get() = LibraryFragmentFactory(fragmentMap)

    private val module: PlayerModule = LibraryInitializer.playerModule()

    private val pipController: PipController = if (isMinOsForPip) {
        AndroidPipController(fragment.requireActivity())
    } else {
        NoOpPipController()
    }

    private val fragmentMap: Map<Class<out Fragment>, () -> Fragment> get() = mapOf(
        PlayerFragment::class.java to {
            PlayerFragment(
                vmFactory = playerViewModelFactory,
                playerViewWrapperFactory = module.playerViewWrapperFactory,
                pipController = pipController,
                errorRenderer = SnackbarErrorRenderer(),
                playbackUiFactory = DefaultPlaybackUiFactory(
                    playerArguments = playerArguments,
                    activity = fragment.requireActivity(),
                    shareDelegate = LibraryInitializer.shareDelegate(),
                    seekBarListenerFactory = DefaultSeekBarListener.Factory(),
                    timeFormatter = LibraryInitializer.timeFormatter(),
                    pipController = pipController,
                    navigator = navigator,
                    imageLoader = fragment.requireContext().applicationContext.imageLoader
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

    private val navigator: Navigator get() = LibraryNavigator(
        fragment.childFragmentManager,
        R.id.container
    )
}
