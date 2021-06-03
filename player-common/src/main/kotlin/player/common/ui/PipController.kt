package player.common.ui

import kotlinx.coroutines.flow.Flow
import player.common.PlayerEvent

interface PipController {
    fun events(): Flow<PipEvent>
    fun enterPip(isPlaying: Boolean): EnterPipResult
    fun onEvent(playerEvent: PlayerEvent)
    fun isInPip(): Boolean
}

enum class EnterPipResult {
    EnteredPip,
    DidNotEnterPip
}

enum class PipEvent {
    Pause,
    Play
}
