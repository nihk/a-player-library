package player.ui.test

import androidx.fragment.app.FragmentActivity
import player.ui.common.CloseDelegate

class FakeCloseDelegate : CloseDelegate {
    var didClose: Boolean = false
        private set

    override fun onClose(activity: FragmentActivity) {
        didClose = true
    }
}
