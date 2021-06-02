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
    // fixme: i want to have a signal for a PlaybackUi.Factory here, but can't because i'm in :common
    //  one option is use Stringly typed refs to factories, and pass in factories to initializer.
    //  another is use a Class<*>.
    //  ^both aren't ideal.
    //  there might need to be some kind of refactor, e.g. moving playbackUi to common, but that's
    //  not great either.
    //  Maybe downcast to a type with a create(SharedDependencies)?
    data class RelatedMedia(val metadata: List<Metadata>) : PlaybackInfo() {
        data class Metadata(
            val uri: String,
            val imageUri: String,
            val durationMillis: Long
        )
    }
}
