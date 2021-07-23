package player.ui.test

import androidx.activity.ComponentActivity
import player.ui.common.CloseDelegate

class FakeCloseDelegate : CloseDelegate {
    var didClose: Boolean = false
        private set

    override fun onClose(activity: ComponentActivity) {
        didClose = true
    }
}
