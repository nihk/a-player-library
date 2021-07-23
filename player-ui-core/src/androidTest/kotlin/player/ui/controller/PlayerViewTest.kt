package player.ui.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import player.common.DefaultPlaybackInfoResolver
import player.common.PlayerEvent
import player.common.PlayerException
import player.test.FakeAppPlayer
import player.test.FakeAppPlayerFactory
import player.test.FakePlayerEventDelegate
import player.test.FakePlayerEventStream
import player.test.FakePlayerViewWrapper
import player.test.FakeSeekDataUpdater
import player.ui.common.PictureInPictureConfig
import player.ui.common.PlayerArguments
import player.ui.test.FakePipController
import player.ui.test.FakePlaybackUi
import player.ui.test.FakePlaybackUiFactory

class PlayerViewTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun appPlayerWasTornDownWhenFragmentIsDestroyed() = playerView {
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
    fun appPlayerWasNotReleasedAcrossFragmentRecreation() = playerView {
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
    fun errorEmissionRendersMessage() = playerView {
        emit(PlayerEvent.OnPlayerError(PlayerException("Message")))
        assertErrorMessageRendered("Message")
    }

    @Test
    fun pipEnabledStartsPipOnBackPress() = playerView(pipOnBackPress = true) {
        Espresso.pressBack()

        assertPipOnBackPress(didEnter = true)
    }

    private fun playerView(
        pipOnBackPress: Boolean = false,
        block: PlayerViewRobot.() -> Unit
    ) {
        val pipConfig = PictureInPictureConfig(
            onBackPresses = pipOnBackPress
        )
        PlayerViewRobot(pipConfig).block()
    }

    private class PlayerViewRobot(
        val pipConfig: PictureInPictureConfig
    ) {
        private val appPlayer = FakeAppPlayer()
        private val appPlayerFactory = FakeAppPlayerFactory(appPlayer)
        private val playerViewWrapper = FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())
        private val eventFlow = MutableStateFlow<PlayerEvent?>(null)
        private val playerEventStream = FakePlayerEventStream(eventFlow.filterNotNull())
        private val playerEventDelegate = FakePlayerEventDelegate()
        private val pipController = FakePipController()
        private val pipControllerFactory = FakePipController.Factory(pipController)
        private val errorRenderer = FakeErrorRenderer()
        private val playbackInfoResolver = DefaultPlaybackInfoResolver()
        private val seekDataUpdater = FakeSeekDataUpdater()
        private val scenario: FragmentScenario<TestFragment>
        private val playbackUi = FakePlaybackUi()
        private val playbackUiFactory = FakePlaybackUiFactory(playbackUi)
        private val playerNonConfigFactory = PlayerNonConfig.Factory(
            appPlayerFactory = appPlayerFactory,
            playerEventStream = playerEventStream,
            playerEventDelegate = playerEventDelegate,
            playbackInfoResolver = playbackInfoResolver,
            seekDataUpdater = seekDataUpdater
        )
        private val vmFactory = PlayerViewModel.Factory(
            playerNonConfigFactory = playerNonConfigFactory
        )
        private val playerViewWrapperFactory = FakePlayerViewWrapper.Factory(playerViewWrapper)
        private val args = PlayerArguments(
            id = "id",
            uri = "",
            pipConfig = pipConfig,
            playbackUiFactory = FakePlaybackUiFactory::class.java
        )
        private val playerViewFactory = PlayerView.Factory(
            vmFactory = vmFactory,
            playerViewWrapperFactory = playerViewWrapperFactory,
            errorRenderer = errorRenderer,
            pipControllerFactory = pipControllerFactory,
            playbackUiFactories = listOf(playbackUiFactory)
        )
        private val id = "id"

        init {
            scenario = launchFragmentInContainer {
                TestFragment(
                    playerViewFactory = playerViewFactory,
                    playerArguments = args
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

class TestFragment(
    private val playerViewFactory: PlayerView.Factory,
    private val playerArguments: PlayerArguments
) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return playerViewFactory.create(
            requireContext(),
            playerArguments
        )
    }
}
