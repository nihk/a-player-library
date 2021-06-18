package player.ui.controller

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import player.common.AppPlayer
import player.common.DefaultPlaybackInfoResolver
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerException
import player.common.PlayerViewWrapper
import player.common.ShareDelegate
import player.common.TrackInfo
import player.test.NoOpPlayerViewWrapper
import player.ui.common.Navigator
import player.ui.common.PictureInPictureConfig
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.common.PlayerController
import player.ui.common.SharedDependencies
import player.ui.common.toBundle

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
        private val pipControllerFactory = FakePipController.Factory()
        private val errorRenderer = FakeErrorRenderer()
        private val playbackInfoResolver = DefaultPlaybackInfoResolver()
        private val seekDataUpdater = FakeSeekDataUpdater()
        private val timeFormatter = FakeTimeFormatter()
        private val navigator = NoOpNavigator()
        private val seekBarListenerFactory = DefaultSeekBarListener.Factory()
        private val scenario: FragmentScenario<PlayerFragment>
        private val playbackUi = FakePlaybackUi()
        private val playbackUiFactory = FakePlaybackUiFactory(playbackUi)

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
                        navigator = navigator
                    ),
                    pipControllerFactory = pipControllerFactory,
                    playbackUiFactories = listOf(playbackUiFactory)
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
            assertEquals(times, playbackUi.attachCount)
        }

        fun assertPlayerDetached(times: Int) {
            assertEquals(times, playbackUi.detachCount)
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

class FakePlayerViewWrapperFactory(
    private val playerViewWrapper: PlayerViewWrapper
) : PlayerViewWrapper.Factory {
    override fun create(context: Context) = playerViewWrapper
}

class FakePipController : PipController {
    override fun events(): Flow<PipController.Event> = emptyFlow()
    override fun enterPip() = PipController.Result.EnteredPip
    override fun isInPip(): Boolean = false
    override fun onEvent(playerEvent: PlayerEvent) = Unit

    class Factory : PipController.Factory {
        override fun create(playerController: PlayerController): PipController {
            return FakePipController()
        }
    }
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
    override fun toTracksPicker(trackInfos: List<TrackInfo>) = Unit
}

class FakePlaybackUi : PlaybackUi {
    var attachCount: Int = 0
    var detachCount: Int = 0
    override val view: View get() = FrameLayout(ApplicationProvider.getApplicationContext())

    override fun onPlayerEvent(playerEvent: PlayerEvent) = Unit
    override fun onUiState(uiState: player.ui.common.UiState) = Unit
    override fun onTracksState(tracksState: player.ui.common.TracksState) = Unit
    override fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>) = Unit
    override fun saveState(): Bundle = Bundle()
    override fun attach(appPlayer: AppPlayer) {
        ++attachCount
    }
    override fun detachPlayer() {
        ++detachCount
    }
}

class FakePlaybackUiFactory(private val playbackUi: PlaybackUi) : PlaybackUi.Factory {
    override fun create(
        deps: SharedDependencies,
        playerViewWrapper: PlayerViewWrapper,
        pipController: PipController,
        playerController: PlayerController,
        playerArguments: PlayerArguments,
        registryOwner: SavedStateRegistryOwner
    ): PlaybackUi {
        return playbackUi
    }
}
