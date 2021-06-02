package nick.sample.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import player.common.PlaybackInfo
import player.common.PlaybackInfoResolver
import player.ui.def.DefaultPlaybackUi
import player.ui.sve.SvePlaybackUi
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SlowPlaybackInfoResolver : PlaybackInfoResolver {
    override fun playbackInfos(uri: String): Flow<PlaybackInfo> = flow {
        val mediaUri = PlaybackInfo.MediaUri(uri)
        val relatedMedia = PlaybackInfo.RelatedMedia(
            metadata = listOf(
                PlaybackInfo.RelatedMedia.Metadata(
                    uri = "https://multiplatform-f.akamaihd.net/i/multi/april11/cctv/cctv_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8",
                    imageUri = "https://i.imgur.com/MYmm7E1.jpg",
                    durationMillis = TimeUnit.SECONDS.toMillis(141L),
                    playbackUiFactory = SvePlaybackUi.Factory::class.java
                ),
                PlaybackInfo.RelatedMedia.Metadata(
                    uri = "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/MI201109210084_mpeg-4_hd_high_1080p25_10mbits.mp4",
                    imageUri = "https://i.imgur.com/ecnSVVk.jpg",
                    durationMillis = TimeUnit.SECONDS.toMillis(43L),
                    playbackUiFactory = DefaultPlaybackUi.Factory::class.java
                ),
                PlaybackInfo.RelatedMedia.Metadata(
                    uri = "https://bestvpn.org/html5demos/assets/dizzy.mp4",
                    imageUri = "https://i.imgur.com/cB7oqeF.jpeg",
                    durationMillis = TimeUnit.SECONDS.toMillis(55L),
                    playbackUiFactory = SvePlaybackUi.Factory::class.java
                ),
                PlaybackInfo.RelatedMedia.Metadata(
                    uri = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
                    imageUri = "https://i.imgur.com/9OPnZNk.png",
                    durationMillis = TimeUnit.SECONDS.toMillis(13L),
                    playbackUiFactory = DefaultPlaybackUi.Factory::class.java
                ),
            )
        )
        emit(PlaybackInfo.Batched(listOf(mediaUri, relatedMedia)))

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
