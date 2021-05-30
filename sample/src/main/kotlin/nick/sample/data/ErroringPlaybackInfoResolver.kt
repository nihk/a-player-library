package nick.sample.data

import kotlinx.coroutines.delay
import player.common.PlaybackInfo
import player.common.PlaybackInfoResolver
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ErroringPlaybackInfoResolver : PlaybackInfoResolver {
    override suspend fun resolve(uri: String): PlaybackInfo {
        delay(5.toDuration(DurationUnit.SECONDS))
        throw RuntimeException("Failed to resolve media!")
    }
}
