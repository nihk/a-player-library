package player.ui.controller

import androidx.lifecycle.SavedStateHandle
import player.common.PlayerState
import player.common.TrackInfo

class PlayerSavedState(
    id: String,
    private val handle: SavedStateHandle
) {
    val manuallySetTracks: List<TrackInfo>
        get() = handle[keyManuallySetTrackInfos] ?: emptyList()

    val state: PlayerState
        get() = handle[keyPlayerState] ?: PlayerState.INITIAL

    fun save(playerState: PlayerState?, tracks: List<TrackInfo>) {
        handle[keyPlayerState] = playerState
        handle[keyManuallySetTrackInfos] = tracks.filter(TrackInfo::isManuallySet)
    }

    fun clear() {
        handle.remove<PlayerState>(keyPlayerState)
        handle.remove<List<TrackInfo>>(keyManuallySetTrackInfos)
    }

    private val keyManuallySetTrackInfos = "$id-manually_set_track_infos"
    private val keyPlayerState = "$id-player_state"
}
