package player.ui.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import player.common.PlayerEvent
import player.ui.common.PipController
import player.ui.common.PlayerController

class FakePipController : PipController {
    var didEnterPip: Boolean = false
        private set
    override fun events(): Flow<PipController.Event> = emptyFlow()
    override fun enterPip(): PipController.Result {
        didEnterPip = true
        return PipController.Result.EnteredPip
    }
    override fun isInPip(): Boolean = false
    override fun onEvent(playerEvent: PlayerEvent) = Unit

    class Factory(private val pipController: PipController) : PipController.Factory {
        override fun create(playerController: PlayerController): PipController {
            return pipController
        }
    }
}
