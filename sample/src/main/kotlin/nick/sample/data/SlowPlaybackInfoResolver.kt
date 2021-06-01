package nick.sample.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import player.common.PlaybackInfo
import player.common.PlaybackInfoResolver
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SlowPlaybackInfoResolver : PlaybackInfoResolver {
    override fun playbackInfos(uri: String): Flow<PlaybackInfo> = flow {
        emit(PlaybackInfo.MediaUri(uri))

        delay(2.toDuration(DurationUnit.SECONDS))
        val captions = PlaybackInfo.Captions(
            metadata = listOf(
                PlaybackInfo.Captions.Metadata(
                    uri = "https://gist.githubusercontent.com/samdutton/ca37f3adaf4e23679957b8083e061177/raw/e19399fbccbc069a2af4266e5120ae6bad62699a/sample.vtt",
                    mimeType = "text/vtt",
                    language = "en"
                )
            )
        )
        val title = PlaybackInfo.MediaTitle("This is a resolved title")
        val batched = PlaybackInfo.Batched(listOf(captions, title))
        emit(batched)
    }.flowOn(Dispatchers.IO)
}
