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

    data class Captions(
        val metadata: List<Metadata>,
        val mediaUriRef: String
    ) : PlaybackInfo() {
        data class Metadata(
            val uri: String,
            val mimeType: String,
            val language: String
        )
    }

    data class MediaTitle(
        val title: String,
        val mediaUriRef: String
    ) : PlaybackInfo()

    data class RelatedMedia(
        val imageUri: String,
        val durationMillis: Long,
        val uri: String
    ) : PlaybackInfo()
}
