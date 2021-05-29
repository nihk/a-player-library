package library.ui

import androidx.lifecycle.SavedStateHandle
import library.common.PlayerState
import library.common.TrackInfo

class PlayerSavedState(private val handle: SavedStateHandle) {
    fun manuallySetTracks(): List<TrackInfo> {
        return handle[KEY_MANUALLY_SET_TRACK_INFOS] ?: emptyList()
    }

    fun playerState(): PlayerState? {
        return handle[KEY_PLAYER_STATE]
    }

    fun save(playerState: PlayerState?, tracks: List<TrackInfo>) {
        handle[KEY_PLAYER_STATE] = playerState
        handle[KEY_MANUALLY_SET_TRACK_INFOS] = tracks.filter(TrackInfo::isManuallySet)
    }

    companion object {
        private const val KEY_PLAYER_STATE = "player_state"
        private const val KEY_MANUALLY_SET_TRACK_INFOS = "manually_set_track_infos"
    }
}
