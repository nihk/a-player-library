package player.ui.def

import player.common.TrackInfo
import player.ui.trackspicker.TracksPickerConfig

class FakeTracksPickerConfigFactory : TracksPickerConfigFactory {
    override fun create(type: TrackInfo.Type): TracksPickerConfig {
        return TracksPickerConfig(type, 0)
    }
}
