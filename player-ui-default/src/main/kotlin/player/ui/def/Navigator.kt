package player.ui.def

import androidx.fragment.app.FragmentManager
import player.common.TrackInfo
import player.ui.trackspicker.TracksPickerFragment

interface Navigator {
    fun toTracksPicker(type: TrackInfo.Type)

    companion object {
        operator fun invoke(fragmentManager: FragmentManager): Navigator = Default(fragmentManager)
    }

    class Default(private val fragmentManager: FragmentManager) : Navigator {
        override fun toTracksPicker(type: TrackInfo.Type) {
            TracksPickerFragment.create(type).show(fragmentManager, null)
        }
    }
}
