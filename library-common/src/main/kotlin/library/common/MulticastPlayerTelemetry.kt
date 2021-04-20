package library.common

class MulticastPlayerTelemetry(
    private val playerTelemetrys: List<PlayerTelemetry>
) : PlayerTelemetry {

    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        playerTelemetrys.forEach { playerTelemetry ->
            playerTelemetry.onPlayerEvent(playerEvent)
        }
    }

    class Builder {
        private val playerTelemetrys = mutableListOf<PlayerTelemetry>()

        fun add(playerTelemetry: PlayerTelemetry?) = apply {
            if (playerTelemetry == null) return@apply
            playerTelemetrys += playerTelemetry
        }

        fun build(): PlayerTelemetry {
            return MulticastPlayerTelemetry(playerTelemetrys)
        }
    }
}