package player.ui.common

import player.common.TrackInfo

interface Navigator {
    fun toTracksPicker(trackInfos: List<TrackInfo>)
}
