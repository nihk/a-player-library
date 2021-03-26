package library.common

import android.content.Context
import android.view.View

interface PlayerViewWrapper {
    val view: View

    fun bindTextTracksPicker(textTracks: (View) -> Unit)
    fun bindAudioTracksPicker(audioTracks: (View) -> Unit)
    fun bindVideoTracksPicker(videoTracks: (View) -> Unit)
    fun bindShare(onClick: (View) -> Unit)
    fun bindPlay(play: (View) -> Unit)
    fun bindPause(pause: (View) -> Unit)

    fun attachTo(appPlayer: AppPlayer)
    fun detach()

    fun onEvent(playerEvent: PlayerEvent) = Unit

    interface Factory {
        fun create(context: Context): PlayerViewWrapper
    }
}