package library.exoplayer

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.TrackNameProvider
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import library.common.AppPlayer
import library.common.PlayerState
import library.test.NoOpPlayerViewWrapper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import okio.source
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
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
}

class FakeTrackNameProvider : TrackNameProvider {
    override fun getTrackName(format: Format): String {
        return format.label ?: "unknown"
    }
}

fun webServer(block: suspend AssetWebServerRobot.() -> Unit) = runBlocking {
    val robot = AssetWebServerRobot()
    try {
        robot.block()
    } finally {
        robot.release()
    }
}

class AssetWebServerRobot {
    private val server = MockWebServer()

    fun start(customResponseCodes: Map<String, Int> = emptyMap()): String {
        server.dispatcher = createAssetDispatcher(customResponseCodes)
        server.start(port = 8080)
        return server.url("/").toString()
    }

    fun release() {
        server.shutdown()
    }

    private fun createAssetDispatcher(customResponseCodes: Map<String, Int>): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = requireNotNull(request.path)

                val customResponseCode = customResponseCodes[path]
                if (customResponseCode != null) {
                    return MockResponse().setResponseCode(customResponseCode)
                }

                val assetPath = if (path.startsWith("/")) {
                    path.drop(1)
                } else {
                    path
                }

                val stream = ApplicationProvider.getApplicationContext<Application>()
                    .assets
                    .open(assetPath)
                val buffer = Buffer().apply {
                    writeAll(stream.source())
                }
                return MockResponse().setBody(buffer)
            }
        }
    }
}

fun player(block: suspend ExoPlayerWrapperRobot.() -> Unit) = runBlocking {
    val robot = ExoPlayerWrapperRobot()
    try {
        robot.block()
    } finally {
        robot.release()
    }
}

class ExoPlayerWrapperRobot(private val context: CoroutineContext = Dispatchers.Main) {
    private val appContext: Context get() = ApplicationProvider.getApplicationContext()
    private var appPlayer: AppPlayer? = null

    suspend fun assertVideoTracks(count: Int) = withContext(context) {
        assertEquals(count, appPlayer!!.videoTracks.size)
    }

    suspend fun assertTextTracks(count: Int) = withContext(context) {
        assertEquals(count, appPlayer!!.textTracks.size)
    }

    suspend fun assertAudioTracks(count: Int) = withContext(context) {
        assertEquals(count, appPlayer!!.audioTracks.size)
    }

    suspend fun play(
        uri: String,
        vttCaptions: String? = null
    ) = withContext(context) {
        val player = createPlayer(uri, vttCaptions)
        appPlayer = ExoPlayerWrapper(player, FakeTrackNameProvider())
        appPlayer!!.bind(NoOpPlayerViewWrapper(), PlayerState.INITIAL)
        player.awaitPlaying()
    }

    suspend fun release() = withContext(context) {
        appPlayer!!.release()
        appPlayer = null
    }

    private fun createPlayer(
        uri: String,
        vttCaptions: String? = null
    ): Player {
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

        return SimpleExoPlayer.Builder(appContext)
            .build()
            .apply {
                setMediaItem(mediaItem)
            }
    }

    private suspend fun Player.awaitPlaying() = suspendCancellableCoroutine<Unit> { continuation ->
        val listener = object : Player.EventListener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
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