package player.core

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import player.common.TrackInfo
import player.ui.controller.PlayerFragment
import player.ui.controller.TracksPickerFragment
import player.ui.shared.Navigator
import player.ui.shared.PlayerArguments
import player.ui.shared.toBundle

internal class LibraryNavigator(
    private val fragmentManager: FragmentManager,
    @IdRes private val containerId: Int
) : Navigator {
    override fun toTracksPicker(trackInfos: List<TrackInfo>) {
        fragmentManager.commit {
            add(TracksPickerFragment::class.java, TracksPickerFragment.args(trackInfos), null)
        }
    }

    override fun toPlayer(playerArguments: PlayerArguments) {
        fragmentManager.commit {
            replace(containerId, PlayerFragment::class.java, playerArguments.toBundle())
            addToBackStack(null)
        }
    }
}
