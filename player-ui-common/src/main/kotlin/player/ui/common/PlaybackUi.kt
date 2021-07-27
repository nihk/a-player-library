package player.ui.common

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.savedstate.SavedStateRegistry
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerViewWrapper
import player.common.SeekData

interface PlaybackUi : SavedStateRegistry.SavedStateProvider {
    val view: View

    fun onPlayerEvent(playerEvent: PlayerEvent)
    fun onUiState(uiState: UiState)
    fun onSeekData(seekData: SeekData)
    fun onTracksState(tracksState: TracksState)
    fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>)
    fun attach(appPlayer: AppPlayer)
    fun detachPlayer()

    override fun saveState(): Bundle = Bundle.EMPTY

    interface Factory {
        fun create(
            activity: ComponentActivity,
            playerViewWrapperFactory: PlayerViewWrapper.Factory,
            playerController: PlayerController,
            playerArguments: PlayerArguments
        ): PlaybackUi
    }
}
