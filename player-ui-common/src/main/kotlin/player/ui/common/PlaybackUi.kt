package player.ui.common

import android.os.Bundle
import android.view.View
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import player.common.PlaybackInfo
import player.common.PlayerEvent

interface PlaybackUi : SavedStateRegistry.SavedStateProvider {
    val view: View

    fun onPlayerEvent(playerEvent: PlayerEvent)
    fun onUiState(uiState: UiState)
    fun onTracksState(tracksState: TracksState)
    fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>)

    override fun saveState() = Bundle()

    interface Factory {
        fun create(
            deps: SharedDependencies,
            pipController: PipController,
            playerController: PlayerController,
            playerArguments: PlayerArguments,
            registryOwner: SavedStateRegistryOwner
        ): PlaybackUi
    }
}
