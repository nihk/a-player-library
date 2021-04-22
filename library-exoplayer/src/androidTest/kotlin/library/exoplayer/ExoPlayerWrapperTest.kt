package library.exoplayer

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.TrackNameProvider
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import library.common.AppPlayer
import library.common.PlayerState
import library.test.NoOpPlayerViewWrapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedReader
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

class ExoPlayerWrapperTest {
    @Test
    fun validateTracksForSimpleMp4() = player {
        play(uri = "dizzy.mp4".asset())

        assertVideoTracks(count = 1)
        assertTextTracks(count = 0)
        assertAudioTracks(count = 1)
    }

    @Test
    fun validateTracksForMp4WithSubtitles() = player {
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
    fun validateTracksForHls_onLocalWebServer() {
        webServer {
            serveAsset("offline_hls/master.m3u8")
            player {
                play(uri = this@webServer.uri)

                assertVideoTracks(count = 4)
                assertTextTracks(count = 0)
                assertAudioTracks(count = 1)
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

fun webServer(block: suspend MockWebServerRobot.() -> Unit) = runBlocking {
    val robot = MockWebServerRobot()
    try {
        robot.block()
    } finally {
        robot.release()
    }
}

class MockWebServerRobot {
    private val server = MockWebServer()
    private lateinit var path: String
    private val httpUrl by lazy { server.url(path) }
    val uri: String get() = httpUrl.toString()

    fun serveAsset(assetName: String) {
        path = "/$assetName"

        val body = ApplicationProvider.getApplicationContext<Application>()
            .assets
            .open(assetName)
            .bufferedReader()
            .use(BufferedReader::readText)
        server.enqueue(MockResponse().setBody(body))
        server.start(port = 8080)
    }

    fun release() {
        server.shutdown()
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
        waitUntil { appPlayer!!.state.isPlaying }
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

    private suspend fun waitUntil(
        howLong: Duration = 5.seconds,
        pollDelay: Duration = 1.milliseconds,
        condition: () -> Boolean
    ) {
        withTimeout(howLong) {
            while (!condition()) {
                delay(pollDelay)
            }
        }
    }
}