package nick.sample.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import player.common.PlaybackInfo
import player.common.PlaybackInfoResolver
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ErroringPlaybackInfoResolver : PlaybackInfoResolver {
    override fun playbackInfos(uri: String): Flow<PlaybackInfo> = flow {
        delay(5.toDuration(DurationUnit.SECONDS))
        error("Failed to resolve media!")
    }
}
