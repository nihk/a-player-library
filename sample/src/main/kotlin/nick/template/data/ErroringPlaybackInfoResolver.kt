package nick.template.data

import kotlinx.coroutines.delay
import library.common.PlaybackInfoResolver
import library.common.PlaybackInfo
import kotlin.time.seconds

class ErroringPlaybackInfoResolver : PlaybackInfoResolver {
    override suspend fun resolve(uri: String): PlaybackInfo {
        delay(5.seconds)
        throw RuntimeException("Failed to resolve media!")
    }
}
