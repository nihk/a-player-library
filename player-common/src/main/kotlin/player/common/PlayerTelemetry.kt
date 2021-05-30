package player.common

interface PlayerTelemetry {
    suspend fun onPlayerEvent(playerEvent: PlayerEvent)
}