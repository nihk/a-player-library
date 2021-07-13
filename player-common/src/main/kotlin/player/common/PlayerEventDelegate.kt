package player.common

interface PlayerEventDelegate {
    suspend fun onPlayerEvent(playerEvent: PlayerEvent)
}