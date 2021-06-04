package player.core

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import player.common.TrackInfo
import player.ui.common.Navigator
import player.ui.controller.TracksPickerFragment

internal class LibraryNavigator(
    private val fragmentManager: FragmentManager,
    @IdRes private val containerId: Int
) : Navigator {
    override fun toTracksPicker(trackInfos: List<TrackInfo>) {
        fragmentManager.commit {
            add(TracksPickerFragment::class.java, TracksPickerFragment.args(trackInfos), null)
        }
    }
}
