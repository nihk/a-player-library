package player.ui.test

import androidx.fragment.app.FragmentActivity
import androidx.savedstate.SavedStateRegistryOwner
import player.common.PlayerViewWrapper
import player.ui.common.Navigator
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.common.PlayerController

class FakePlaybackUiFactory(private val playbackUi: PlaybackUi) : PlaybackUi.Factory {
    override fun create(
        activity: FragmentActivity,
        navigator: Navigator,
        playerViewWrapperFactory: PlayerViewWrapper.Factory,
        pipController: PipController,
        playerController: PlayerController,
        playerArguments: PlayerArguments,
        registryOwner: SavedStateRegistryOwner
    ): PlaybackUi {
        return playbackUi
    }
}
