package player.ui

import player.common.PlayerArguments
import player.common.TrackInfo

interface Navigator {
    fun toTracksPicker(trackInfos: List<TrackInfo>)
    fun toPlayer(playerArguments: PlayerArguments)
}
