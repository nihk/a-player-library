package player.test

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import player.common.PlayerViewWrapper

class FakePlayerViewWrapper(context: Context) : NoOpPlayerViewWrapper() {
    override val view: View = FrameLayout(context)

    override fun detachPlayer() {
        super.detachPlayer()
        // Support reusing the same test View across Fragment recreation.
        (view.parent as? ViewGroup)?.removeView(view)
    }

    class Factory(
        private val playerViewWrapper: PlayerViewWrapper
    ) : PlayerViewWrapper.Factory {
        override fun create(context: Context) = playerViewWrapper
    }
}
