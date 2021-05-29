package library.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import library.CoroutinesTestRule
import library.common.PictureInPictureConfig
import library.common.PlayerArguments
import library.common.PlayerEvent
import library.common.PlayerException
import library.common.PlayerViewWrapper
import library.common.ShareDelegate
import library.common.TrackInfo
import library.common.toBundle
import library.test.NoOpPlayerViewWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerFragmentTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun appPlayerWasTornDownWhenFragmentIsDestroyed() = playerFragment {
        assertPlayerCreated(times = 1)
        assertPlayerNotReleased()
        destroy()
        assertPlayerReleased()
    }

    @Test
    fun appPlayerWasNotReleasedAcrossFragmentRecreation() = playerFragment {
        assertPlayerCreated(times = 1)
        recreate()
        assertPlayerCreated(times = 1)
        assertViewDetached()
        assertPlayerNotReleased()
    }

    @Test
    fun errorEmissionRendersMessage() = playerFragment {
        emit(PlayerEvent.OnPlayerError(PlayerException("Message")))
        assertErrorMessageRendered("Message")
    }
}

class FakePlayerViewWrapper(context: Context) : NoOpPlayerViewWrapper() {
    override val view: View = FrameLayout(context)

    override fun detach() {
        super.detach()
        // Support reusing the same test View across Fragment recreation.
        (view.parent as? ViewGroup)?.removeView(view)
    }
}

class FakePlayerViewWrapperFactory(val playerViewWrapper: PlayerViewWrapper) : PlayerViewWrapper.Factory {
    override fun create(context: Context) = playerViewWrapper
}

class FakePipController(
    private val flow: Flow<PipEvent> = emptyFlow()
) : PipController {
    override fun events() = flow
    override fun enterPip(isPlaying: Boolean) = EnterPipResult.EnteredPip
    override fun isInPip(): Boolean = false
    override fun onEvent(playerEvent: PlayerEvent) = Unit
}

class FakeErrorRenderer : ErrorRenderer {
    var didRender: Boolean = false
    var collectedMessage: String? = null

    override fun render(view: View, message: String) {
        didRender = true
        collectedMessage = message
    }
}

class NoOpNavigator : Navigator {
    override fun toDialog(clazz: Class<out Fragment>, bundle: Bundle?) = Unit
    override fun replace(clazz: Class<out Fragment>, bundle: Bundle?) = Unit
}

fun playerFragment(block: PlayerFragmentRobot.() -> Unit) {
    PlayerFragmentRobot().block()
}

class PlayerFragmentRobot {
    private val appPlayer = FakeAppPlayer()
    private val appPlayerFactory = FakeAppPlayerFactory(appPlayer)
    private val playerViewWrapper = FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())
    private val eventFlow = MutableStateFlow<PlayerEvent?>(null)
    private val playerEventStream = FakePlayerEventStream(eventFlow.filterNotNull())
    private val telemetry = FakePlayerTelemetry()
    private val shareDelegate: ShareDelegate? = null
    private val pipConfig = PictureInPictureConfig(false, false)
    private val pipController = FakePipController()
    private val errorRenderer = FakeErrorRenderer()
    private val playbackInfoResolver = NoOpPlaybackInfoResolver()
    private val seekDataUpdater = FakeSeekDataUpdater()
    private val timeFormatter = FakeTimeFormatter()
    private val navigator = NoOpNavigator()
    private val scenario: FragmentScenario<PlayerFragment>

    init {
        val vmFactory = PlayerViewModel.Factory(
            appPlayerFactory = appPlayerFactory,
            playerEventStream = playerEventStream,
            telemetry = telemetry,
            playbackInfoResolver = playbackInfoResolver,
            seekDataUpdater = seekDataUpdater
        )

        val playerViewWrapperFactory = FakePlayerViewWrapperFactory(playerViewWrapper)

        val args = PlayerArguments(
            uri = "",
            pipConfig = pipConfig
        )
        scenario = launchFragmentInContainer(fragmentArgs = args.toBundle()) {
            PlayerFragment(vmFactory, playerViewWrapperFactory, shareDelegate, pipController, errorRenderer, navigator, timeFormatter)
        }
    }

    fun destroy() {
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }

    fun recreate() {
        scenario.recreate()
    }

    fun emit(playerEvent: PlayerEvent) {
        eventFlow.value = playerEvent
    }

    fun setTrackCount(text: Int = 0, audio: Int = 0, video: Int = 0) {
        addTracks(TrackInfo.Type.TEXT, text)
        addTracks(TrackInfo.Type.AUDIO, audio)
        addTracks(TrackInfo.Type.VIDEO, video)
    }

    private fun addTracks(type: TrackInfo.Type, amount: Int) {
        repeat(amount) {
            appPlayer.fakeTracks += TrackInfo("", type, 0, 0, 0, false, false, false, false)
        }
    }

    fun assertPlayerCreated(times: Int) {
        assertEquals(times, appPlayerFactory.createCount)
    }

    fun assertViewDetached() {
        assertTrue(playerViewWrapper.didDetach)
    }

    fun assertPlayerReleased() {
        assertTrue(appPlayer.didRelease)
    }

    fun assertPlayerNotReleased() {
        assertFalse(appPlayer.didRelease)
    }

    fun assertErrorMessageRendered(string: String) {
        assertEquals(string, errorRenderer.collectedMessage)
        assertTrue(errorRenderer.didRender)
    }
}