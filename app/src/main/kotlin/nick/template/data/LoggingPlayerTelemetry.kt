package nick.template.data

import android.util.Log
import library.common.PlayerEvent
import library.common.PlayerTelemetry
import library.common.TAG

class LoggingPlayerTelemetry : PlayerTelemetry {
    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        Log.d(TAG, playerEvent.toString())
    }
}