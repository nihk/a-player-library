package player.ui.test

import androidx.activity.ComponentActivity
import player.common.PlayerViewWrapper
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.common.PlayerController

class FakePlaybackUiFactory(private val playbackUi: PlaybackUi) : PlaybackUi.Factory {
    override fun create(
        activity: ComponentActivity,
        playerViewWrapperFactory: PlayerViewWrapper.Factory,
        pipController: PipController,
        playerController: PlayerController,
        playerArguments: PlayerArguments,
    ): PlaybackUi {
        return playbackUi
    }
}
