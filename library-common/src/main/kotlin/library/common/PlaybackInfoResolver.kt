package library.common

interface PlaybackInfoResolver {
    suspend fun resolve(uri: String): PlaybackInfo
}

class DefaultPlaybackInfoResolver : PlaybackInfoResolver {
    override suspend fun resolve(uri: String): PlaybackInfo {
        return PlaybackInfo(uri)
    }
}
