package player.common

import android.content.Context
import android.view.View

interface PlayerViewWrapper {
    val view: View

    fun attach(appPlayer: AppPlayer)
    fun detachPlayer()

    fun onEvent(playerEvent: PlayerEvent) = Unit

    interface Factory {
        fun create(context: Context): PlayerViewWrapper
    }
}