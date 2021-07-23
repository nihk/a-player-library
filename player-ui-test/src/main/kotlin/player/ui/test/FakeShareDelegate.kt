package player.ui.test

import androidx.activity.ComponentActivity
import player.ui.common.ShareDelegate

class FakeShareDelegate : ShareDelegate {
    var didShare: Boolean = false
        private set

    override fun share(activity: ComponentActivity, uri: String) {
        didShare = true
    }
}
