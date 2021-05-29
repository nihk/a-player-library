package library.test

import android.view.View
import library.common.AppPlayer
import library.common.PlayerEvent
import library.common.PlayerViewWrapper
import library.common.TrackInfo

open class NoOpPlayerViewWrapper : PlayerViewWrapper {
    var didAttach: Boolean = false
    var didDetach: Boolean = false
    val boundTrackTypes = mutableSetOf<TrackInfo.Type>()

    override val view: View get() = error("unused")

    override fun onEvent(playerEvent: PlayerEvent) = Unit

    override fun attachTo(appPlayer: AppPlayer) {
        didAttach = true
    }

    override fun detach() {
        didDetach = true
    }
}
