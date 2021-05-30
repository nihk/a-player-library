package nick.sample.data

import kotlinx.coroutines.delay
import player.common.PlaybackInfo
import player.common.PlaybackInfoResolver
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SlowPlaybackInfoResolver : PlaybackInfoResolver {
    override suspend fun resolve(uri: String): PlaybackInfo {
        delay(2.toDuration(DurationUnit.SECONDS)) // Simulate work
        return PlaybackInfo(uri)
    }
}
