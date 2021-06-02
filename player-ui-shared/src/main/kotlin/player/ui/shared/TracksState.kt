package player.ui.shared

import player.common.TrackInfo

sealed class TracksState {
    data class Available(val trackTypes: List<TrackInfo.Type>) : TracksState()
    object NotAvailable : TracksState()
}
