package player.ui.def

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import player.common.TrackInfo
import player.ui.trackspicker.TracksPickerFragment

interface Navigator {
    fun toTracksPicker(trackInfos: List<TrackInfo>)

    companion object {
        operator fun invoke(fragmentManager: FragmentManager): Navigator = Default(fragmentManager)
    }

    class Default(private val fragmentManager: FragmentManager) : Navigator {
        override fun toTracksPicker(trackInfos: List<TrackInfo>) {
            fragmentManager.commit {
                add(TracksPickerFragment::class.java, TracksPickerFragment.args(trackInfos), null)
            }
        }
    }
}
