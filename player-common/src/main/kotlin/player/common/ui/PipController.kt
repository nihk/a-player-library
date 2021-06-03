package player.common.ui

import kotlinx.coroutines.flow.Flow
import player.common.PlayerEvent

interface PipController {
    fun events(): Flow<Event>
    fun enterPip(isPlaying: Boolean): Result
    fun onEvent(playerEvent: PlayerEvent)
    fun isInPip(): Boolean

    enum class Result {
        EnteredPip,
        DidNotEnterPip
    }
    enum class Event {
        Pause,
        Play
    }
}
