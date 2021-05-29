package library.mediaplayer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import library.common.AppPlayer
import library.common.SeekDataUpdater
import library.common.SeekData

class MediaPlayerSeekDataUpdater : SeekDataUpdater {
    override fun seekData(appPlayer: AppPlayer): Flow<SeekData> {
        // todo
        return emptyFlow()
    }
}