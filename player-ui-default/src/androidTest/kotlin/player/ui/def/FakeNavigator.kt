package player.ui.def

import player.common.TrackInfo

class FakeNavigator : Navigator {
    override fun toTracksPicker(type: TrackInfo.Type, onDismissed: () -> Unit) {
        error("unused")
    }
}
