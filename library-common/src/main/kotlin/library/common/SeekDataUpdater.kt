package library.common

import kotlinx.coroutines.flow.Flow

interface SeekDataUpdater {
    fun seekData(appPlayer: AppPlayer): Flow<SeekData>
}
