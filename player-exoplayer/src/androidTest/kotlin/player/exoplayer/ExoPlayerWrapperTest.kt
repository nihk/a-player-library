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
import org.junit.Assert.fail
import org.junit.Test
import player.common.AppPlayer
import player.common.PlayerState
import player.common.TrackInfo
import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class ExoPlayerWrapperTest {
    @Test
    fun validateTracksForMp4() = player {
        play(uri = "dizzy.mp4".asset())

        assertVideoTracks(count = 1)
        assertTextTracks(count = 0)
        assertAudioTracks(count = 1)
    }

    @Test
    fun validateTracksForMp4_withSubtitles() = player {
        play(uri = "dizzy.mp4".asset(), vttCaptions = "vtt-captions".asset())

        assertVideoTracks(count = 1)
        assertTextTracks(count = 1)
        assertAudioTracks(count = 1)
    }

    @Test
    fun validateTracksForHls() = player {
        play(uri = "offline_hls/master.m3u8".asset())

        assertVideoTracks(count = 4)
        assertTextTracks(count = 0)
        assertAudioTracks(count = 1)
    }

    @Test
    fun validateTracksForMp4_onLocalWebServer() {
        webServer {
            val baseUrl = start()
            player {
                play(uri = "${baseUrl}dizzy.mp4")

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
                play(uri = "${baseUrl}offline_hls/master.m3u8")

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
                play(uri = "${baseUrl}offline_hls/master_with_missing_file.m3u8")

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
                play(uri = "$baseUrl$path")

                fail("Expected exception was not thrown")
            }
        }
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
        val robot = ExoPlayerWrapperRobot()
        try {
            robot.block()
        } finally {
            robot.release()
        }
    }

    private class ExoPlayerWrapperRobot(private val context: CoroutineContext = Dispatchers.Main) {
        private val appContext: Context get() = ApplicationProvider.getApplicationContext()
        private var appPlayer: AppPlayer? = null

        suspend fun assertVideoTracks(count: Int) = withContext(context) {
            assertEquals(count, appPlayer!!.tracks.filter { it.type == TrackInfo.Type.VIDEO }.size)
        }

        suspend fun assertTextTracks(count: Int) = withContext(context) {
            assertEquals(count, appPlayer!!.tracks.filter { it.type == TrackInfo.Type.TEXT }.size)
        }

        suspend fun assertAudioTracks(count: Int) = withContext(context) {
            assertEquals(count, appPlayer!!.tracks.filter { it.type == TrackInfo.Type.AUDIO }.size)
        }

        suspend fun play(
            uri: String,
            vttCaptions: String? = null
        ) = withContext(context) {
            val player = createPlayer(uri, vttCaptions)
            appPlayer = ExoPlayerWrapper(player, FakeTrackNameProvider(), PlayerState.INITIAL)
            player.awaitPlaying()
        }

        suspend fun release() = withContext(context) {
            appPlayer!!.release()
            appPlayer = null
        }

        private fun createPlayer(
            uri: String,
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
            val dataSourceFactory = DefaultDataSourceFactory(appContext, null, httpDataSourceFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, DefaultExtractorsFactory()).apply {
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
                    setMediaItem(mediaItem)
                    prepare()
                }
        }

        private suspend fun Player.awaitPlaying() = suspendCancellableCoroutine<Unit> { continuation ->
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