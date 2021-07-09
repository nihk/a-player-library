package player.ui.trackspicker

import player.common.TrackInfo

internal sealed class TrackOption {
    data class Auto(
        val name: String,
        val isSelected: Boolean,
        val rendererIndex: Int
    ) : TrackOption()

    data class SingleTrack(val trackInfo: TrackInfo) : TrackOption()
}
