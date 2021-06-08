package player.exoplayer

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.TrackNameProvider
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import okio.source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import player.common.PlaybackInfo
import player.common.PlayerState
import player.common.TrackInfo
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class ExoPlayerWrapperTest {
    @Test
    fun validateTracksForMp4() = player {
        createPlayer(uri = "dizzy.mp4".asset(), awaitPlaying = true)

        assertVideoTracks(count = 1)
        assertTextTracks(count = 0)
        assertAudioTracks(count = 1)
    }

    @Test
    fun validateTracksForMp4_withSubtitles() = player {
        createPlayer(
            uri = "dizzy.mp4".asset(),
            vttCaptions = "vtt-captions".asset(),
            awaitPlaying = true
        )

        assertVideoTracks(count = 1)
        assertTextTracks(count = 1)
        assertAudioTracks(count = 1)
    }

    @Test
    fun validateTracksForHls() = player {
        createPlayer(uri = "offline_hls/master.m3u8".asset(), awaitPlaying = true)

        assertVideoTracks(count = 4)
        assertTextTracks(count = 0)
        assertAudioTracks(count = 1)
    }

    @Test
    fun validateTracksForMp4_onLocalWebServer() {
        webServer {
            val baseUrl = start()
            player {
                createPlayer(uri = "${baseUrl}dizzy.mp4", awaitPlaying = true)

                assertVideoTracks(count = 1)
                assertTextTracks(count = 0)
                assertAudioTracks(count = 1)
            }
        }
    }

    @Test
    fun validateTracksForHls_onLocalWebServer() {
        webServer {
            val baseUrl = start()
            player {
                createPlayer(uri = "${baseUrl}offline_hls/master.m3u8", awaitPlaying = true)

                assertVideoTracks(count = 4)
                assertTextTracks(count = 0)
                assertAudioTracks(count = 1)
            }
        }
    }

    @Test(expected = ExoPlaybackException::class)
    fun missingM3u8Throws_onLocalWebServer() {
        webServer {
            val baseUrl = start()
            player {
                createPlayer(
                    uri = "${baseUrl}offline_hls/master_with_missing_file.m3u8",
                    awaitPlaying = true
                )

                fail("Expected exception was not thrown")
            }
        }
    }

    @Test(expected = ExoPlaybackException::class)
    fun playerThrowsWhenLocalServerRespondsWithClientError() {
        webServer {
            val path = "offline_hls/master.m3u8"
            val baseUrl = start(customResponseCodes = mapOf("/$path" to 404))
            player {
                createPlayer(uri = "$baseUrl$path", awaitPlaying = true)

                fail("Expected exception was not thrown")
            }
        }
    }

    @Test
    fun mediaUriPlaybackInfoAddsMediaItem() = player {
        val wrapper = createPlayer()
        val uri = "dizzy.mp4".asset()
        val mediaUri = PlaybackInfo.MediaUri(uri = uri)

        wrapper.handlePlaybackInfos(listOf(mediaUri))

        assertNotNull(wrapper.player.currentMediaItem)
        assertEquals(uri.toUri(), wrapper.player.currentMediaItem?.playbackProperties?.uri)
        assertEquals(1, wrapper.player.mediaItemCount)
    }

    @Test
    fun captionsPlaybackInfoBeforeMediaUriDoesNotAddsMediaItem() = player {
        val captionsUri = "vtt-captions".asset()
        val captions = PlaybackInfo.Captions(
            metadata = listOf(
                PlaybackInfo.Captions.Metadata(
                    uri = captionsUri,
                    mimeType = "text/vtt",
                    language = "en"
                )
            ),
            mediaUriRef = "dizzy.mp4".asset()
        )
        val wrapper = createPlayer()

        wrapper.handlePlaybackInfos(listOf(captions))

        assertNull(wrapper.player.currentMediaItem)
        assertEquals(0, wrapper.player.mediaItemCount)
    }

    @Test
    fun captionsPlaybackInfoWithMediaUriAddsMediaItemWithSubtitles() = player {
        val uri = "dizzy.mp4".asset()
        val mediaUri = PlaybackInfo.MediaUri(uri = uri)
        val captionsUri = "vtt-captions".asset()
        val captions = PlaybackInfo.Captions(
            metadata = listOf(
                PlaybackInfo.Captions.Metadata(
                    uri = captionsUri,
                    mimeType = "text/vtt",
                    language = "en"
                )
            ),
            mediaUriRef = "dizzy.mp4".asset()
        )
        val wrapper = createPlayer()

        wrapper.handlePlaybackInfos(listOf(captions, mediaUri))

        assertNotNull(wrapper.player.currentMediaItem)
        assertEquals(uri.toUri(), wrapper.player.currentMediaItem?.playbackProperties?.uri)
        assertEquals(captionsUri.toUri(), wrapper.player.currentMediaItem?.playbackProperties?.subtitles?.first()?.uri)
        assertEquals(1, wrapper.player.mediaItemCount)
    }

    @Test
    fun relatedMediaPlaybackInfoAddsMediaItems() = player {
        val mp4 = "dizzy.mp4".asset()
        val hls = "offline_hls/master.m3u8".asset()
        val relatedMedia = listOf(
            PlaybackInfo.RelatedMedia(
                uri = mp4,
                imageUri = "",
                durationMillis = TimeUnit.SECONDS.toMillis(141L)
            ),
            PlaybackInfo.RelatedMedia(
                uri = hls,
                imageUri = "",
                durationMillis = TimeUnit.SECONDS.toMillis(43L)
            ),
        )
        val wrapper = createPlayer()

        wrapper.handlePlaybackInfos(relatedMedia)

        assertEquals(2, wrapper.player.mediaItemCount)
        val first = wrapper.player.getMediaItemAt(0)
        val second = wrapper.player.getMediaItemAt(1)
        assertEquals(mp4.toUri(), first.playbackProperties?.uri)
        assertEquals(hls.toUri(), second.playbackProperties?.uri)
    }

    @Test
    fun captionsAfterMediaUriUpdatesMediaUri() = player {
        val uri = "dizzy.mp4".asset()
        val mediaUri = PlaybackInfo.MediaUri(uri = uri)
        val captionsUri = "vtt-captions".asset()
        val captions = PlaybackInfo.Captions(
            metadata = listOf(
                PlaybackInfo.Captions.Metadata(
                    uri = captionsUri,
                    mimeType = "text/vtt",
                    language = "en"
                )
            ),
            mediaUriRef = "dizzy.mp4".asset()
        )
        val wrapper = createPlayer()

        wrapper.handlePlaybackInfos(listOf(mediaUri))
        wrapper.handlePlaybackInfos(listOf(mediaUri, captions))

        assertNotNull(wrapper.player.currentMediaItem)
        assertEquals(uri.toUri(), wrapper.player.currentMediaItem?.playbackProperties?.uri)
        assertEquals(captionsUri.toUri(), wrapper.player.currentMediaItem?.playbackProperties?.subtitles?.first()?.uri)
        assertEquals(1, wrapper.player.mediaItemCount)
    }

    private fun String.asset(): String {
        return "asset:///$this"
    }

    private fun webServer(block: AssetWebServerRobot.() -> Unit) {
        AssetWebServerRobot().use { it.block() }
    }

    private class AssetWebServerRobot private constructor(
        private val server: MockWebServer
    ) : Closeable by server {

        constructor() : this(MockWebServer())

        fun start(customResponseCodes: Map<String, Int> = emptyMap()): String {
            server.dispatcher = createAssetDispatcher(customResponseCodes)
            server.start(port = 8080) // Use a consistent port for easier debugging
            return server.url("/").toString()
        }

        private fun createAssetDispatcher(customResponseCodes: Map<String, Int>): Dispatcher {
            return object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = requireNotNull(request.path)

                    val customResponseCode = customResponseCodes[path]
                    if (customResponseCode != null) {
                        return MockResponse().setResponseCode(customResponseCode)
                    }

                    // Android asset paths can't start with "/"
                    val assetPath = if (path.startsWith("/")) {
                        path.drop(1)
                    } else {
                        path
                    }

                    val stream = ApplicationProvider.getApplicationContext<Application>()
                        .assets
                        .open(assetPath)

                    // Convert the InputStream to okio APIs, which MockResponse uses
                    val buffer = Buffer().apply {
                        writeAll(stream.source())
                    }
                    return MockResponse().setBody(buffer)
                }
            }
        }
    }

    private fun player(block: suspend ExoPlayerWrapperRobot.() -> Unit) = runBlocking {
        withContext(Dispatchers.Main) {
            val robot = ExoPlayerWrapperRobot()
            try {
                robot.block()
            } finally {
                robot.release()
            }
        }
    }

    private class ExoPlayerWrapperRobot(private val context: CoroutineContext = Dispatchers.Main) {
        private val appContext: Context get() = ApplicationProvider.getApplicationContext()
        private var appPlayer: ExoPlayerWrapper? = null

        fun assertVideoTracks(count: Int) {
            assertEquals(count, appPlayer!!.tracks.filter { it.type == TrackInfo.Type.VIDEO }.size)
        }

        fun assertTextTracks(count: Int) {
            assertEquals(count, appPlayer!!.tracks.filter { it.type == TrackInfo.Type.TEXT }.size)
        }

        fun assertAudioTracks(count: Int) {
            assertEquals(count, appPlayer!!.tracks.filter { it.type == TrackInfo.Type.AUDIO }.size)
        }

        suspend fun createPlayer(
            uri: String? = null,
            vttCaptions: String? = null,
            awaitPlaying: Boolean = false
        ): ExoPlayerWrapper {
            val player = createExoPlayer(uri, vttCaptions)
            appPlayer = ExoPlayerWrapper(player, FakeTrackNameProvider(), PlayerState.INITIAL)
            if (awaitPlaying) {
                player.awaitPlaying()
            }
            return appPlayer!!
        }

        fun release() {
            appPlayer?.release()
            appPlayer = null
        }

        private fun createExoPlayer(
            uri: String? = null,
            vttCaptions: String? = null
        ): ExoPlayer {
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .apply {
                    if (vttCaptions != null) {
                        val subtitle = MediaItem.Subtitle(
                            vttCaptions.toUri(),
                            MimeTypes.TEXT_VTT,
                            "en",
                            C.SELECTION_FLAG_DEFAULT
                        )
                        setSubtitles(listOf(subtitle))
                    }
                }
                .build()

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setReadTimeoutMs(500) // Fail fast!
            val dataSourceFactory =
                DefaultDataSourceFactory(appContext, null, httpDataSourceFactory)
            val mediaSourceFactory =
                DefaultMediaSourceFactory(dataSourceFactory, DefaultExtractorsFactory()).apply {
                    // Don't retry at all
                    setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
                        override fun getMinimumLoadableRetryCount(dataType: Int) = 0
                        override fun getRetryDelayMsFor(
                            loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
                        ) = C.TIME_UNSET
                    })
                }

            return SimpleExoPlayer.Builder(
                appContext,
                DefaultRenderersFactory(appContext),
                DefaultTrackSelector(appContext),
                mediaSourceFactory,
                DefaultLoadControl(),
                DefaultBandwidthMeter.getSingletonInstance(appContext),
                AnalyticsCollector(Clock.DEFAULT)
            )
                .build()
                .apply {
                    if (uri != null) {
                        setMediaItem(mediaItem)
                    }
                    prepare()
                }
        }

        private suspend fun Player.awaitPlaying() =
            suspendCancellableCoroutine<Unit> { continuation ->
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            removeListener(this)
                            continuation.resume(Unit)
                        }
                    }

                    override fun onPlayerError(error: ExoPlaybackException) {
                        removeListener(this)
                        continuation.cancel(error)
                    }
                }

                addListener(listener)

                continuation.invokeOnCancellation { removeListener(listener) }
            }
    }
}

class FakeTrackNameProvider : TrackNameProvider {
    override fun getTrackName(format: Format): String {
        return format.label ?: "unknown"
    }
}
