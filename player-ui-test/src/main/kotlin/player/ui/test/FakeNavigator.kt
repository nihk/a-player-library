package player.ui.test

import player.common.TrackInfo
import player.ui.common.Navigator

class FakeNavigator : Navigator {
    override fun toTracksPicker(trackInfos: List<TrackInfo>) {
        error("unused")
    }
}
