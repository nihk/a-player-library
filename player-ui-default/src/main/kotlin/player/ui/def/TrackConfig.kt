package player.ui.def

import player.common.TrackInfo

data class TrackConfig(
    val type: TrackInfo.Type,
    val filter: (TrackInfo) -> Boolean = { true }
) {
    companion object Factory {
        fun create(type: TrackInfo.Type): TrackConfig {
            return when (type) {
                TrackInfo.Type.VIDEO -> TrackConfig(type) { it.size.height > 0 } // Omit audio only tracks
                TrackInfo.Type.AUDIO -> TrackConfig(type)
                TrackInfo.Type.TEXT -> TrackConfig(type)
            }
        }
    }
}
