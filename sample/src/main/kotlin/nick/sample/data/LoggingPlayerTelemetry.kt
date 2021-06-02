package nick.sample.data

import android.util.Log
import player.common.PlayerEvent
import player.common.PlayerTelemetry

class LoggingPlayerTelemetry : PlayerTelemetry {
    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        Log.d("asdf", playerEvent.toString())
    }
}
