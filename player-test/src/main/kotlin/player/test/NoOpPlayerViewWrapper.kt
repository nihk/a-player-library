package player.test

import android.view.View
import player.common.AppPlayer
import player.common.PlayerEvent
import player.common.ui.PlayerViewWrapper

open class NoOpPlayerViewWrapper : PlayerViewWrapper {
    var attachCount: Int = 0
    var detachCount: Int = 0

    override val view: View get() = error("unused")

    override fun onEvent(playerEvent: PlayerEvent) = Unit

    override fun attach(appPlayer: AppPlayer) {
        ++attachCount
    }

    override fun detachPlayer() {
        ++detachCount
    }
}
