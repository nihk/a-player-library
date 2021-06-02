package player.ui.controller

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import player.common.DefaultPlaybackInfoResolver
import player.ui.shared.PictureInPictureConfig
import player.common.PlayerEvent
import player.common.PlayerException
import player.common.PlayerViewWrapper
import player.common.ShareDelegate
import player.common.TrackInfo
import player.test.NoOpPlayerViewWrapper
import player.ui.shared.DefaultSeekBarListener
import player.ui.shared.EnterPipResult
import player.ui.shared.Navigator
import player.ui.shared.PipController
import player.ui.shared.PipEvent
import player.ui.shared.PlaybackUi
import player.ui.shared.PlayerArguments
import player.ui.shared.PlayerController
import player.ui.shared.PlayerViewModel
import player.ui.shared.SharedDependencies
import player.ui.shared.TracksState
import player.ui.shared.UiState
import player.ui.shared.toBundle

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
        private val shareDelegate: ShareDelegate? = null
        private val pipConfig = PictureInPictureConfig(enabled = false, onBackPresses = false)
        private val pipController = FakePipController()
        private val errorRenderer = FakeErrorRenderer()
        private val playbackInfoResolver = DefaultPlaybackInfoResolver()
        private val seekDataUpdater = FakeSeekDataUpdater()
        private val timeFormatter = FakeTimeFormatter()
        private val navigator = NoOpNavigator()
        private val seekBarListenerFactory = DefaultSeekBarListener.Factory()
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
                pipConfig = pipConfig,
                playbackUiFactory = FakePlaybackUiFactory::class.java
            )
            scenario = launchFragmentInContainer(fragmentArgs = args.toBundle()) {
                PlayerFragment(
                    vmFactory = vmFactory,
                    playerViewWrapperFactory = playerViewWrapperFactory,
                    errorRenderer = errorRenderer,
                    deps = SharedDependencies(
                        shareDelegate = shareDelegate,
                        context = ApplicationProvider.getApplicationContext(),
                        seekBarListenerFactory = seekBarListenerFactory,
                        timeFormatter = timeFormatter,
                        pipController = pipController,
                        navigator = navigator
                    )
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
    override fun pop(): Boolean = false
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

class FakePlaybackUiFactory : PlaybackUi.Factory {
    override fun create(
        deps: SharedDependencies,
        playerController: PlayerController,
        playerArguments: PlayerArguments
    ): PlaybackUi {
        return FakePlaybackUi()
    }
}
