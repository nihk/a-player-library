package player.ui.playbackui

import android.view.View
import androidx.fragment.app.FragmentActivity
import coil.ImageLoader
import player.common.PlaybackUiType
import player.common.PlayerArguments
import player.common.ShareDelegate
import player.common.TimeFormatter
import player.ui.Navigator
import player.ui.PipController
import player.ui.PlayerController
import player.ui.PlayerEmissionsHandler
import player.ui.SeekBarListener
import player.ui.playbackui.shortvideoexperience.SvePlaybackUi

interface PlaybackUi : PlayerEmissionsHandler {
    val view: View

    interface Factory {
        fun create(playerController: PlayerController): PlaybackUi
    }
}

class DefaultPlaybackUiFactory(
    private val playerArguments: PlayerArguments,
    private val activity: FragmentActivity,
    private val shareDelegate: ShareDelegate?,
    private val seekBarListenerFactory: SeekBarListener.Factory,
    private val timeFormatter: TimeFormatter,
    private val pipController: PipController,
    private val navigator: Navigator,
    private val imageLoader: ImageLoader
) : PlaybackUi.Factory {
    override fun create(playerController: PlayerController): PlaybackUi {
        return when (playerArguments.playbackUiType) {
            PlaybackUiType.Default -> DefaultPlaybackUi(
                playerArguments,
                playerController,
                activity,
                shareDelegate,
                seekBarListenerFactory,
                timeFormatter,
                pipController,
                navigator
            )
            PlaybackUiType.ShortVideoExperience -> SvePlaybackUi(
                playerArguments,
                playerController,
                activity,
                shareDelegate,
                seekBarListenerFactory,
                timeFormatter,
                pipController,
                navigator,
                imageLoader
            )
        }
    }
}
