package player.core

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import player.common.PlayerArguments
import player.common.TrackInfo
import player.common.toBundle
import player.ui.Navigator
import player.ui.PlayerFragment
import player.ui.TracksPickerFragment

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
