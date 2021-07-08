package player.test

import player.common.PlayerEvent
import player.common.PlayerTelemetry

class FakePlayerTelemetry : PlayerTelemetry {
    val collectedEvents = mutableListOf<PlayerEvent>()
    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        collectedEvents.add(playerEvent)
    }
}
