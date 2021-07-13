package player.test

import player.common.PlayerEvent
import player.common.PlayerEventDelegate

class FakePlayerEventDelegate : PlayerEventDelegate {
    val collectedEvents = mutableListOf<PlayerEvent>()
    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        collectedEvents.add(playerEvent)
    }
}
