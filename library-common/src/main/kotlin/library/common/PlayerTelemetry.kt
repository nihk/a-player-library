package library.common

interface PlayerTelemetry {
    suspend fun onPlayerEvent(playerEvent: PlayerEvent)
}