package player.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import player.common.AppPlayer
import player.common.SeekData
import player.common.SeekDataUpdater

class FakeSeekDataUpdater : SeekDataUpdater {
    override fun seekData(appPlayer: AppPlayer): Flow<SeekData> {
        return emptyFlow()
    }
}
