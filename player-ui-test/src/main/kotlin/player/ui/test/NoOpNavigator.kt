package player.ui.test

import player.common.TrackInfo
import player.ui.common.Navigator

class NoOpNavigator : Navigator {
    override fun toTracksPicker(trackInfos: List<TrackInfo>) = Unit
}
