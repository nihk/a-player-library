package player.core

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import player.common.TrackInfo
import player.ui.controller.PlayerFragment
import player.ui.controller.TracksPickerFragment
import player.common.ui.Navigator
import player.common.ui.PlayerArguments
import player.common.ui.toBundle

// todo: fragment transitions/animations
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

    override fun pop(): Boolean {
        return if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
            true
        } else {
            false
        }
    }
}
