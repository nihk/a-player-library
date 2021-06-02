package player.common

sealed class PlaybackInfo {
    data class Batched(val playbackInfos: List<PlaybackInfo>) : PlaybackInfo() {
        init {
            if (playbackInfos.any { it is Batched }) {
                error("Do not nest Batched inside Batched")
            }
        }
    }
    data class MediaUri(val uri: String) : PlaybackInfo()
    data class Captions(val metadata: List<Metadata>) : PlaybackInfo() {
        data class Metadata(
            val uri: String,
            val mimeType: String,
            val language: String
        )
    }
    data class MediaTitle(val title: String) : PlaybackInfo()
    data class RelatedMedia(val metadata: List<Metadata>) : PlaybackInfo() {
        data class Metadata(
            val uri: String,
            val imageUri: String,
            val durationMillis: Long,
            val playbackUiFactory: Class<*>
        )
    }
}
