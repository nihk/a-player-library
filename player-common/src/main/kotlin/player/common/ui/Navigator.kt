package player.common.ui

import player.common.TrackInfo

interface Navigator {
    fun toTracksPicker(trackInfos: List<TrackInfo>)
    fun toPlayer(playerArguments: PlayerArguments)
    fun pop(): Boolean
}
