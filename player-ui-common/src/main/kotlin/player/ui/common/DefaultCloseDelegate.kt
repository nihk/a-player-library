package player.ui.common

import androidx.fragment.app.FragmentActivity
import player.common.CloseDelegate

class DefaultCloseDelegate : CloseDelegate {
    override fun onClose(activity: FragmentActivity) {
        activity.onBackPressed()
    }
}
