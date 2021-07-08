package player.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import player.common.AppPlayer
import player.common.PlayerEvent
import player.common.PlayerEventStream

class FakePlayerEventStream(val flow: Flow<PlayerEvent> = emptyFlow()) : PlayerEventStream {
    override fun listen(appPlayer: AppPlayer) = flow
}
