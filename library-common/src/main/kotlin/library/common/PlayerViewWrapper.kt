package library.common

import android.content.Context
import android.view.View

interface PlayerViewWrapper {
    val view: View

    fun bindTracks(type: TrackInfo.Type, onClick: (View) -> Unit)
    fun bindShare(onClick: (View) -> Unit)
    fun bindPlay(play: (View) -> Unit)
    fun bindPause(pause: (View) -> Unit)

    fun attachTo(appPlayer: AppPlayer)
    fun detach()

    fun onEvent(playerEvent: PlayerEvent) = Unit
    fun setControllerUsability(isUsable: Boolean)
    fun setLoading(isLoading: Boolean)

    interface Factory {
        fun create(context: Context): PlayerViewWrapper
    }
}