package player.ui.shared

import android.view.View
import player.common.PlayerEvent

interface PlaybackUi {
    val view: View

    fun onPlayerEvent(playerEvent: PlayerEvent)
    fun onUiState(uiState: UiState)
    fun onTracksState(tracksState: TracksState)

    interface Factory {
        fun create(
            deps: SharedDependencies,
            playerController: PlayerController
        ): PlaybackUi
    }
}
