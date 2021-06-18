package player.ui.common

import kotlinx.coroutines.flow.Flow
import player.common.PlayerEvent

interface PipController {
    fun events(): Flow<Event>
    fun enterPip(): Result
    fun onEvent(playerEvent: PlayerEvent)
    fun isInPip(): Boolean

    enum class Result {
        EnteredPip,
        DidNotEnterPip
    }

    enum class Event {
        Play,
        Pause
    }

    interface Factory {
        fun create(playerController: PlayerController): PipController
    }
}
