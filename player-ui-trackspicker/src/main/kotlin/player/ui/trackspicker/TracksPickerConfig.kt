package player.ui.trackspicker

import androidx.annotation.StringRes
import player.common.TrackInfo

data class TracksPickerConfig(
    val type: TrackInfo.Type,
    @StringRes val title: Int,
    val withAuto: Boolean = true,
    // E.g. for video tracks, picking 720 would enable that track plus every other playback
    // quality below it, for better adaptive playback.
    val cascadePicks: Boolean = false,
    val mapper: (List<TrackInfo>) -> List<TrackInfo> = { it }
)
