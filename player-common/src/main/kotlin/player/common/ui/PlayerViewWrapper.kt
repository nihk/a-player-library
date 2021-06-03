package player.common.ui

import android.content.Context
import android.view.View
import player.common.AppPlayer
import player.common.PlayerEvent

interface PlayerViewWrapper {
    val view: View

    fun attach(appPlayer: AppPlayer)
    fun detachPlayer()

    fun onEvent(playerEvent: PlayerEvent) = Unit

    interface Factory {
        fun create(context: Context): PlayerViewWrapper
    }
}