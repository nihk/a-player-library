package player.ui.controller

import android.view.View
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import player.common.CloseDelegate
import player.common.DefaultPlaybackInfoResolver
import player.common.PlayerEvent
import player.common.PlayerException
import player.common.ShareDelegate
import player.test.FakeAppPlayer
import player.test.FakeAppPlayerFactory
import player.test.FakeImageLoader
import player.test.FakePlayerEventStream
import player.test.FakePlayerTelemetry
import player.test.FakePlayerViewWrapper
import player.test.FakeSeekDataUpdater
import player.test.FakeTimeFormatter
import player.ui.common.PictureInPictureConfig
import player.ui.common.PlayerArguments
import player.ui.common.SharedDependencies
import player.ui.common.toBundle
import player.ui.test.FakePipController
import player.ui.test.FakePlaybackUi
import player.ui.test.FakePlaybackUiFactory
import player.ui.test.NoOpNavigator

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

    @Test
    fun pipEnabledStartsPipOnBackPress() = playerFragment(pipOnBackPress = true) {
        Espresso.pressBack()

        assertPipOnBackPress(didEnter = true)
    }

    private fun playerFragment(
        pipOnBackPress: Boolean = false,
        block: PlayerFragmentRobot.() -> Unit
    ) {
        val pipConfig = PictureInPictureConfig(
            enabled = pipOnBackPress,
            onBackPresses = pipOnBackPress
        )
        PlayerFragmentRobot(pipConfig).block()
    }

    private class PlayerFragmentRobot(
        val pipConfig: PictureInPictureConfig
    ) {
        private val appPlayer = FakeAppPlayer()
        private val appPlayerFactory = FakeAppPlayerFactory(appPlayer)
        private val playerViewWrapper = FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())
        private val eventFlow = MutableStateFlow<PlayerEvent?>(null)
        private val playerEventStream = FakePlayerEventStream(eventFlow.filterNotNull())
        private val telemetry = FakePlayerTelemetry()
        private val shareDelegate: ShareDelegate? = null
        private val pipController = FakePipController()
        private val pipControllerFactory = FakePipController.Factory(pipController)
        private val errorRenderer = FakeErrorRenderer()
        private val playbackInfoResolver = DefaultPlaybackInfoResolver()
        private val seekDataUpdater = FakeSeekDataUpdater()
        private val timeFormatter = FakeTimeFormatter()
        private val navigator = NoOpNavigator()
        private val seekBarListenerFactory = DefaultSeekBarListener.Factory()
        private val scenario: FragmentScenario<PlayerFragment>
        private val playbackUi = FakePlaybackUi()
        private val playbackUiFactory = FakePlaybackUiFactory(playbackUi)
        private val closeDelegate = CloseDelegate()
        private val imageLoader = FakeImageLoader()

        init {
            val vmFactory = PlayerViewModel.Factory(
                appPlayerFactory = appPlayerFactory,
                playerEventStream = playerEventStream,
                telemetry = telemetry,
                playbackInfoResolver = playbackInfoResolver,
                seekDataUpdater = seekDataUpdater
            )

            val playerViewWrapperFactory = FakePlayerViewWrapper.Factory(playerViewWrapper)

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
                        closeDelegate = closeDelegate,
                        seekBarListenerFactory = seekBarListenerFactory,
                        timeFormatter = timeFormatter,
                        navigator = navigator,
                        imageLoader = imageLoader
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

        fun assertPipOnBackPress(didEnter: Boolean) {
            assertEquals(didEnter, pipController.didEnterPip)
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
