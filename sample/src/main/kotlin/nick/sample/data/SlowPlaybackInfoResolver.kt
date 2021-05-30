package nick.sample.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import player.common.PlaybackInfo
import player.common.PlaybackInfoResolver
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SlowPlaybackInfoResolver : PlaybackInfoResolver {
    override fun playbackInfos(uri: String): Flow<PlaybackInfo> = flow {
        delay(2.toDuration(DurationUnit.SECONDS))
        emit(PlaybackInfo.MediaUri(uri))
        delay(5.toDuration(DurationUnit.SECONDS))
        emit(PlaybackInfo.MediaTitle("This is a resolved title"))
    }.flowOn(Dispatchers.IO)
}
