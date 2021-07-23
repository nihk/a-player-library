package player.ui.def

import player.common.TrackInfo
import player.ui.trackspicker.TracksPickerConfig

interface TracksPickerConfigFactory {
    fun create(type: TrackInfo.Type): TracksPickerConfig

    companion object {
        operator fun invoke(): TracksPickerConfigFactory = Default()
    }

    class Default : TracksPickerConfigFactory {
        override fun create(type: TrackInfo.Type): TracksPickerConfig {
            return when (type) {
                TrackInfo.Type.VIDEO -> TracksPickerConfig(type, R.string.playback_quality) { tracks ->
                    tracks
                        .filter { it.size.height != -1 } // Omit audio only tracks
                        .sortedByDescending { it.size.width } // Highest to lowest quality
                }
                TrackInfo.Type.AUDIO -> TracksPickerConfig(type, R.string.audio_tracks, withAuto = false)
                TrackInfo.Type.TEXT -> TracksPickerConfig(type, R.string.closed_captions)
            }
        }
    }
}
