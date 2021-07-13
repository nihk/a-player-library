package player.ui.test

import androidx.fragment.app.FragmentActivity
import player.ui.common.ShareDelegate

class FakeShareDelegate : ShareDelegate {
    var didShare: Boolean = false
        private set

    override fun share(activity: FragmentActivity, uri: String) {
        didShare = true
    }
}