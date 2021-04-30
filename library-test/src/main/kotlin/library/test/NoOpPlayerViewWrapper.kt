package library.test

import android.view.View
import library.common.AppPlayer
import library.common.PlayerEvent
import library.common.PlayerViewWrapper

open class NoOpPlayerViewWrapper : PlayerViewWrapper {
    var didAttach: Boolean = false
    var didDetach: Boolean = false
    var didBindTextTracks: Boolean = false
    var didBindAudioTracks: Boolean = false
    var didBindVideoTracks: Boolean = false

    override val view: View get() = error("unused")

    override fun bindTextTracksPicker(textTracks: (View) -> Unit) {
        didBindTextTracks = true
    }
    override fun bindAudioTracksPicker(audioTracks: (View) -> Unit) {
        didBindAudioTracks = true
    }
    override fun bindVideoTracksPicker(videoTracks: (View) -> Unit) {
        didBindVideoTracks = true
    }
    override fun bindPlay(play: (View) -> Unit) = Unit
    override fun bindPause(pause: (View) -> Unit) = Unit
    override fun bindShare(onClick: (View) -> Unit) = Unit
    override fun onEvent(playerEvent: PlayerEvent) = Unit
    override fun setControllerUsability(isUsable: Boolean) = Unit

    override fun attachTo(appPlayer: AppPlayer) {
        didAttach = true
    }

    override fun detach() {
        didDetach = true
    }
}