package player.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import player.common.TrackInfo

interface TracksBroadcaster {
    fun tracks(): Flow<List<TrackInfo>>
}

interface MutableTracksBroadcaster : TracksBroadcaster {
    val tracks: Flow<List<TrackInfo>>
    override fun tracks(): Flow<List<TrackInfo>> = tracks
}

class DefaultTracksBroadcaster : MutableTracksBroadcaster {
    override val tracks: Flow<List<TrackInfo>> = MutableStateFlow(emptyList())
}
