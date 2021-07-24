package player.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface PlaybackInfoResolver {
    fun playbackInfos(uri: String): Flow<PlaybackInfo>

    companion object {
        operator fun invoke(): PlaybackInfoResolver = Default()
    }

    class Default : PlaybackInfoResolver {
        override fun playbackInfos(uri: String): Flow<PlaybackInfo> {
            return flowOf(PlaybackInfo.MediaUri(uri))
        }
    }
}
