package nick.template.data

import kotlinx.coroutines.delay
import library.common.PlaybackInfoResolver
import library.common.PlaybackInfo
import kotlin.time.seconds

class SamplePlaybackInfoResolver : PlaybackInfoResolver {
    override suspend fun resolve(uri: String): PlaybackInfo {
//        delay(2.seconds) // Simulate work
        return PlaybackInfo(uri)
    }
}
