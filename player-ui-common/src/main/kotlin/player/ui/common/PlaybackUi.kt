package player.ui.common

import android.view.View
import player.common.PlaybackInfo
import player.common.PlayerEvent

interface PlaybackUi {
    val view: View

    fun onPlayerEvent(playerEvent: PlayerEvent)
    fun onUiState(uiState: UiState)
    fun onTracksState(tracksState: TracksState)
    fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>)

    interface Factory {
        fun create(
            deps: SharedDependencies,
            playerController: PlayerController,
            playerArguments: PlayerArguments
        ): PlaybackUi
    }
}
