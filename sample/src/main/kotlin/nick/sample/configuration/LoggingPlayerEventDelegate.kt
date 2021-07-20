package nick.sample.configuration

import android.util.Log
import player.common.PlayerEvent
import player.common.PlayerEventDelegate

class LoggingPlayerEventDelegate : PlayerEventDelegate {
    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        Log.d("asdf", playerEvent.toString())
    }
}
