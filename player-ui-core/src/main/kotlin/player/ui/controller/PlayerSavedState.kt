package player.ui.controller

import androidx.lifecycle.SavedStateHandle
import player.common.PlayerState
import player.common.TrackInfo

class PlayerSavedState(private val handle: SavedStateHandle) {
    val manuallySetTracks: List<TrackInfo>
        get() = handle[KEY_MANUALLY_SET_TRACK_INFOS] ?: emptyList()

    val state: PlayerState
        get() = handle[KEY_PLAYER_STATE] ?: PlayerState.INITIAL

    fun save(playerState: PlayerState?, tracks: List<TrackInfo>) {
        handle[KEY_PLAYER_STATE] = playerState
        handle[KEY_MANUALLY_SET_TRACK_INFOS] = tracks.filter(TrackInfo::isManuallySet)
    }

    companion object {
        private const val KEY_PLAYER_STATE = "player_state"
        private const val KEY_MANUALLY_SET_TRACK_INFOS = "manually_set_track_infos"
    }
}
