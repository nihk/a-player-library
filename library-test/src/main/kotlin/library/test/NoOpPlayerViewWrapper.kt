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

    override fun bindTracks(type: TrackInfo.Type, onClick: (View) -> Unit) {
        boundTrackTypes += type
    }

    override fun bindPlay(play: (View) -> Unit) = Unit
    override fun bindPause(pause: (View) -> Unit) = Unit
    override fun bindShare(onClick: (View) -> Unit) = Unit
    override fun onEvent(playerEvent: PlayerEvent) = Unit
    override fun setControllerUsability(isUsable: Boolean) = Unit
    override fun setLoading(isLoading: Boolean) = Unit

    override fun attachTo(appPlayer: AppPlayer) {
        didAttach = true
    }

    override fun detach() {
        didDetach = true
    }
}