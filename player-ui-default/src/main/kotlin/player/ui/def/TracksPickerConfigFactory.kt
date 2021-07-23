package player.ui.def

import player.common.TrackInfo
import player.ui.trackspicker.TracksPickerConfig

interface TracksPickerConfigFactory {
    fun create(type: TrackInfo.Type): TracksPickerConfig

    class Default : TracksPickerConfigFactory {
        override fun create(type: TrackInfo.Type): TracksPickerConfig {
            return when (type) {
                TrackInfo.Type.VIDEO -> TracksPickerConfig(type, R.string.playback_quality) {
                    it.size.height != -1 // Omit audio only tracks
                }
                TrackInfo.Type.AUDIO -> TracksPickerConfig(type, R.string.audio_tracks)
                TrackInfo.Type.TEXT -> TracksPickerConfig(type, R.string.closed_captions)
            }
        }
    }
}
