package player.ui.shared

import player.common.TrackInfo

interface Navigator {
    fun toTracksPicker(trackInfos: List<TrackInfo>)
    fun toPlayer(playerArguments: PlayerArguments)
    fun pop(): Boolean
}
