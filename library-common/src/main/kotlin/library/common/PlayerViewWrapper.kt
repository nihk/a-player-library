package library.common

import android.content.Context
import android.view.View

interface PlayerViewWrapper {
    val view: View

    fun attachTo(appPlayer: AppPlayer)
    fun detach()

    fun onEvent(playerEvent: PlayerEvent) = Unit

    interface Factory {
        fun create(context: Context): PlayerViewWrapper
    }
}