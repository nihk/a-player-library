package player.ui.def

import player.common.TrackInfo

class FakeNavigator : Navigator {
    override fun toTracksPicker(trackInfos: List<TrackInfo>) {
        error("unused")
    }
}
