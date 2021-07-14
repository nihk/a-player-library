package nick.sample.data

import androidx.fragment.app.FragmentActivity
import nick.sample.R
import player.ui.common.CloseDelegate
import player.core.LibraryActivity
import player.core.LibraryView

class SampleCloseDelegate : CloseDelegate {
    override fun onClose(activity: FragmentActivity) {
        if (activity is LibraryActivity) {
            activity.finish()
        } else {
            val libraryView: LibraryView? = activity.findViewById(R.id.library_view)
            if (libraryView?.isPlaying == true) {
                libraryView.stop()
            }
            activity.supportFragmentManager.popBackStack()
        }
    }
}
