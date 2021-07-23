package player.ui.trackspicker

import androidx.annotation.StringRes
import player.common.TrackInfo

data class TracksPickerConfig(
    val type: TrackInfo.Type,
    @StringRes val title: Int,
    val withAuto: Boolean = true,
    val mapper: (List<TrackInfo>) -> List<TrackInfo> = { it }
)
