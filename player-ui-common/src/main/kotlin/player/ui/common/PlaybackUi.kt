package player.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerViewWrapper

interface PlaybackUi : SavedStateRegistry.SavedStateProvider {
    val view: View

    fun onPlayerEvent(playerEvent: PlayerEvent)
    fun onUiState(uiState: UiState)
    fun onTracksState(tracksState: TracksState)
    fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>)
    fun attach(appPlayer: AppPlayer)
    fun detachPlayer()

    override fun saveState() = Bundle()

    interface Factory {
        fun create(
            activity: FragmentActivity,
            navigator: Navigator,
            playerViewWrapperFactory: PlayerViewWrapper.Factory,
            pipController: PipController,
            playerController: PlayerController,
            playerArguments: PlayerArguments,
            registryOwner: SavedStateRegistryOwner
        ): PlaybackUi
    }
}
