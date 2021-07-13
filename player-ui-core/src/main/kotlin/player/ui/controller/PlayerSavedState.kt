package player.ui.controller

import androidx.lifecycle.SavedStateHandle
import player.common.PlayerState
import player.common.TrackInfo
import java.util.*

class PlayerSavedState(
    uuid: UUID,
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

    private val keyManuallySetTrackInfos = "$uuid-manually_set_track_infos"
    private val keyPlayerState = "$uuid-player_state"
}
