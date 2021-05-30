package player.common

sealed class PlaybackInfo {
    data class MediaUri(val uri: String) : PlaybackInfo()
    data class CaptionsUri(val uri: String) : PlaybackInfo()
    data class MediaTitle(val title: String) : PlaybackInfo()
}

