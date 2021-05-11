package library.common

interface PlaybackInfoResolver {
    suspend fun resolve(uri: String): PlaybackInfo
}
