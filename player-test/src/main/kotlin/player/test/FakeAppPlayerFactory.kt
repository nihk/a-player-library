package player.test

import player.common.AppPlayer
import player.common.PlayerState

class FakeAppPlayerFactory(val appPlayer: AppPlayer) : AppPlayer.Factory {
    var createdState: PlayerState? = null
    var createCount = 0
    override fun create(initial: PlayerState): AppPlayer {
        ++createCount
        createdState = initial
        return appPlayer
    }
}
