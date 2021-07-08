package player.test

import androidx.fragment.app.FragmentActivity
import player.common.CloseDelegate

class FakeCloseDelegate : CloseDelegate {
    var didClose: Boolean = false
        private set

    override fun onClose(activity: FragmentActivity) {
        didClose = true
    }
}
