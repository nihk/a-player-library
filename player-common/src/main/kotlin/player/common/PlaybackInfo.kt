package player.common

sealed class PlaybackInfo {
    data class MediaUri(val uri: String) : PlaybackInfo()
    data class Captions(val metadata: List<Metadata>) : PlaybackInfo() {
        data class Metadata(
            val uri: String,
            val mimeType: String,
            val language: String
        )
    }
    data class MediaTitle(val title: String) : PlaybackInfo()
}

