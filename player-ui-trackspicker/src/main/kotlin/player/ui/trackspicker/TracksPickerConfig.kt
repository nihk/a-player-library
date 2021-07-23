package player.ui.trackspicker

import androidx.annotation.StringRes
import player.common.TrackInfo

data class TracksPickerConfig(
    val type: TrackInfo.Type,
    @StringRes val title: Int,
    val filter: (TrackInfo) -> Boolean = { true }
)
