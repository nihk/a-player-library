package player.ui.trackspicker

import player.common.TrackInfo

data class TrackOption(
    val id: Int,
    val name: String,
    val isSelected: Boolean,
    val action: Action
) {
    sealed class Action {
        data class Clear(val rendererIndex: Int) : Action()
        data class Set(val trackInfo: TrackInfo) : Action()
    }
}
