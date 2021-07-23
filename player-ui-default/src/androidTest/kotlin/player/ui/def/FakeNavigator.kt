package player.ui.def

import player.ui.trackspicker.TracksPickerConfig

class FakeNavigator : Navigator {
    override fun toTracksPicker(config: TracksPickerConfig, onDismissed: () -> Unit) {
        error("unused")
    }
}
