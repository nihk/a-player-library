package nick.sample.data

import androidx.fragment.app.FragmentActivity
import player.common.CloseDelegate
import player.core.LibraryActivity

class SampleCloseDelegate : CloseDelegate {
    override fun onClose(activity: FragmentActivity) {
        if (activity is LibraryActivity) {
            activity.finish()
        } else {
            activity.supportFragmentManager.popBackStack()
        }
    }
}
