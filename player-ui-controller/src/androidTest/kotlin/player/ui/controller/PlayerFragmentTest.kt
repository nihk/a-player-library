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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import player.common.DefaultPlaybackInfoResolver
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerException
import player.common.ui.PlayerViewWrapper
import player.common.ShareDelegate
import player.common.TrackInfo
import player.test.NoOpPlayerViewWrapper
import player.common.ui.DefaultSeekBarListener
import player.common.ui.EnterPipResult
import player.common.ui.Navigator
import player.common.ui.PictureInPictureConfig
import player.common.ui.PipController
import player.common.ui.PipEvent
import player.common.ui.PlaybackUi
import player.common.ui.PlayerArguments
import player.common.ui.PlayerController
import player.common.ui.SharedDependencies
import player.common.ui.TracksState
import player.common.ui.UiState
import player.common.ui.toBundle

class PlayerFragmentTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun appPlayerWasTornDownWhenFragmentIsDestroyed() = playerFragment {
        assertPlayerCreated(times = 1)
        assertPlayerAttached(times = 1)
        assertPlayerDetached(times = 0)
        assertPlayerReleased(times = 0)

        destroy()

        assertPlayerCreated(times = 1)
        assertPlayerAttached(times = 1)
        assertPlayerDetached(times = 1)
        assertPlayerReleased(times = 1)
    }

    @Test
    fun appPlayerWasNotReleasedAcrossFragmentRecreation() = playerFragment {
        assertPlayerCreated(times = 1)
        assertPlayerAttached(times = 1)
        assertPlayerDetached(times = 0)
        assertPlayerReleased(times = 0)

        recreate()

        assertPlayerCreated(times = 1)
        assertPlayerAttached(times = 2)
        assertPlayerDetached(times = 1)
        assertPlayerReleased(times = 0)
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
        private val playbackUiFactories = listOf(FakePlaybackUiFactory())
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
                    playbackUiFactories = playbackUiFactories,
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

        fun assertPlayerReleased(times: Int) {
            assertEquals(times, appPlayer.releaseCount)
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

    override fun onPlayerEvent(playerEvent: PlayerEvent) = Unit
    override fun onUiState(uiState: UiState) = Unit
    override fun onTracksState(tracksState: TracksState) = Unit
    override fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>) = Unit
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
