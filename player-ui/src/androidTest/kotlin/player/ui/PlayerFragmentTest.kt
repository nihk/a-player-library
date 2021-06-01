package player.ui

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import player.CoroutinesTestRule
import player.common.DefaultPlaybackInfoResolver
import player.common.PictureInPictureConfig
import player.common.PlayerArguments
import player.common.PlayerEvent
import player.common.PlayerException
import player.common.PlayerViewWrapper
import player.common.TrackInfo
import player.common.toBundle
import player.test.NoOpPlayerViewWrapper
import player.ui.playbackui.PlaybackUi

class PlayerFragmentTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun appPlayerWasTornDownWhenFragmentIsDestroyed() = playerFragment {
        assertPlayerCreated(times = 1)
        assertPlayerAttached(times = 1)
        assertPlayerDetached(times = 0)
        assertPlayerNotReleased()

        destroy()

        assertPlayerCreated(times = 1)
        assertPlayerAttached(times = 1)
        assertPlayerDetached(times = 1)
        assertPlayerReleased()
    }

    @Test
    fun appPlayerWasNotReleasedAcrossFragmentRecreation() = playerFragment {
        assertPlayerCreated(times = 1)
        assertPlayerAttached(times = 1)
        assertPlayerDetached(times = 0)
        assertPlayerNotReleased()

        recreate()

        assertPlayerCreated(times = 1)
        assertPlayerAttached(times = 2)
        assertPlayerDetached(times = 1)
        assertPlayerNotReleased()
    }

    @Test
    fun errorEmissionRendersMessage() = playerFragment {
        emit(PlayerEvent.OnPlayerError(PlayerException("Message")))
        assertErrorMessageRendered("Message")
    }

    private fun playerFragment(block: PlayerFragmentRobot.() -> Unit) {
        PlayerFragmentRobot().block()
    }

    private class PlayerFragmentRobot {
        private val appPlayer = FakeAppPlayer()
        private val appPlayerFactory = FakeAppPlayerFactory(appPlayer)
        private val playerViewWrapper = FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())
        private val eventFlow = MutableStateFlow<PlayerEvent?>(null)
        private val playerEventStream = FakePlayerEventStream(eventFlow.filterNotNull())
        private val telemetry = FakePlayerTelemetry()
        private val pipConfig = PictureInPictureConfig(enabled = false, onBackPresses = false)
        private val pipController = FakePipController()
        private val errorRenderer = FakeErrorRenderer()
        private val playbackInfoResolver = DefaultPlaybackInfoResolver()
        private val seekDataUpdater = FakeSeekDataUpdater()
        private val playbackUi = FakePlaybackUi()
        private val playbackUiFactory = FakePlaybackUiFactory(playbackUi)
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
                PlayerFragment(
                    vmFactory = vmFactory,
                    playerViewWrapperFactory = playerViewWrapperFactory,
                    pipController = pipController,
                    errorRenderer = errorRenderer,
                    playbackUiFactory = playbackUiFactory
                )
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

        fun assertPlayerCreated(times: Int) {
            assertEquals(times, appPlayerFactory.createCount)
        }

        fun assertPlayerAttached(times: Int) {
            assertEquals(times, playerViewWrapper.attachCount)
        }

        fun assertPlayerDetached(times: Int) {
            assertEquals(times, playerViewWrapper.detachCount)
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
}

class FakePlayerViewWrapper(context: Context) : NoOpPlayerViewWrapper() {
    override val view: View = FrameLayout(context)

    override fun detachPlayer() {
        super.detachPlayer()
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
    override fun toPlayer(playerArguments: PlayerArguments) = Unit
    override fun toTracksPicker(trackInfos: List<TrackInfo>) = Unit
}

class FakePlaybackUi : PlaybackUi {
    override val view: View get() = FrameLayout(ApplicationProvider.getApplicationContext())

    override fun onPlayerEvent(playerEvent: PlayerEvent) {
    }

    override fun onUiState(uiState: UiState) {
    }

    override fun onTracksState(tracksState: TracksState) {
    }
}

class FakePlaybackUiFactory(
    private val playbackUi: PlaybackUi = FakePlaybackUi()
) : PlaybackUi.Factory {
    override fun create(playerController: PlayerController): PlaybackUi {
        return playbackUi
    }
}
