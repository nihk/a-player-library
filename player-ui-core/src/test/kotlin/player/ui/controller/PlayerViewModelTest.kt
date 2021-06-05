package player.ui.controller

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import player.common.DefaultPlaybackInfoResolver
import player.common.PlayerEvent
import player.common.PlayerState
import player.ui.common.TracksState

class PlayerViewModelTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun `getPlayer creates a new player when there is no active player`() = playerViewModel {
        getPlayer()
        assertPlayerCreated(times = 1)
    }

    @Test
    fun `multiple getPlayer calls only create player once`() = playerViewModel {
        getPlayer()
        getPlayer()
        assertPlayerCreated(times = 1)
    }

    @Test
    fun `player state is used when creating new player`() = playerViewModel {
        val playerState = PlayerState(itemIndex = 0, positionMillis = 5000L, isPlaying = false)
        setPlayerState(playerState)
        getPlayer()
        assertPlayerCreatedWithState(playerState)
    }

    @Test
    fun `player events can be received when player is created`() = playerViewModel {
        getPlayer()
        emit(PlayerEvent.Initial)
        assertEmission(PlayerEvent.Initial)
    }

    @Test
    fun `player events are not received if player is not created`() = playerViewModel {
        emit(PlayerEvent.Initial)
        assertNoEmission(PlayerEvent.Initial)
    }

    @Test
    fun `player events stop being received after app is backgrounded`() = playerViewModel {
        getPlayer()
        emit(PlayerEvent.Initial)
        onAppBackgrounded()
        val tracksChanged = PlayerEvent.OnTracksChanged(emptyList())
        emit(tracksChanged)
        assertNoEmission(tracksChanged)
    }

    @Test
    fun `app player receives player events`() = playerViewModel {
        getPlayer()
        emit(PlayerEvent.Initial)
        assertAppPlayerEmission(PlayerEvent.Initial)
    }

    @Test
    fun `telemetry receives player events`() = playerViewModel {
        getPlayer()
        emit(PlayerEvent.Initial)
        assertTelemetryEmission(PlayerEvent.Initial)
    }

    @Test
    fun `player did tear down when app is backgrounded`() = playerViewModel {
        getPlayer()
        onAppBackgrounded()
        assertReleased(times = 1)
    }

    @Test
    fun `track state changes according to relevant player event emissions`() = playerViewModel {
        getPlayer()
        assertEmission(TracksState.NotAvailable)
        val event = PlayerEvent.OnTracksChanged(emptyList())
        emit(event)
        assertEmission(event)
    }

    private fun playerViewModel(block: suspend PlayerViewModelRobot.() -> Unit) = coroutinesTestRule.testDispatcher.runBlockingTest {
        PlayerViewModelRobot(this)
            .apply { block() }
            .release()
    }

    private class PlayerViewModelRobot(scope: CoroutineScope) {
        private val playerSavedState = PlayerSavedState(SavedStateHandle())
        private val appPlayer = FakeAppPlayer()
        private val appPlayerFactory = FakeAppPlayerFactory(appPlayer)
        private val events = MutableSharedFlow<PlayerEvent>()
        private val playerEventStream = FakePlayerEventStream(events)
        private val telemetry = FakePlayerTelemetry()
        private val playbackInfoResolver = DefaultPlaybackInfoResolver()
        private val seekDataUpdater = FakeSeekDataUpdater()
        private val viewModel = PlayerViewModel(
            playerSavedState = playerSavedState,
            appPlayerFactory = appPlayerFactory,
            playerEventStream = playerEventStream,
            telemetry = telemetry,
            playbackInfoResolver = playbackInfoResolver,
            uri = "https://www.example.com/video.mp4",
            seekDataUpdater = seekDataUpdater
        )
        private val emittedEvents = mutableListOf<PlayerEvent>()
        private val emittedTrackStates = mutableListOf<TracksState>()
        private val jobs = mutableListOf<Job>()

        init {
            jobs += viewModel.playerEvents()
                .onEach { emittedEvents += it }
                .launchIn(scope)

            jobs += viewModel.tracksStates()
                .onEach { emittedTrackStates += it }
                .launchIn(scope)
        }

        fun release() {
            jobs.forEach(Job::cancel)
        }

        suspend fun emit(playerEvent: PlayerEvent = PlayerEvent.Initial) {
            events.emit(playerEvent)
        }

        fun setPlayerState(playerState: PlayerState?) {
            playerSavedState.save(playerState, emptyList())
        }

        fun getPlayer() {
            viewModel.getPlayer()
        }

        fun onAppBackgrounded() {
            viewModel.onAppBackgrounded()
        }

        fun assertPlayerCreated(times: Int) {
            assertEquals(times, appPlayerFactory.createCount)
        }

        fun assertPlayerCreatedWithState(playerState: PlayerState?) {
            assertEquals(playerState, appPlayerFactory.createdState)
        }

        fun assertEmission(playerEvent: PlayerEvent) {
            assertTrue(playerEvent in emittedEvents)
        }

        fun assertTelemetryEmission(playerEvent: PlayerEvent) {
            assertTrue(playerEvent in telemetry.collectedEvents)
        }

        fun assertAppPlayerEmission(playerEvent: PlayerEvent) {
            assertTrue(playerEvent in appPlayer.collectedEvents)
        }

        fun assertNoEmission(playerEvent: PlayerEvent) {
            assertFalse(playerEvent in emittedEvents)
        }

        fun assertEmission(tracksState: TracksState) {
            assertTrue(tracksState in emittedTrackStates)
        }

        fun assertReleased(times: Int) {
            assertEquals(times, appPlayer.releaseCount)
        }
    }
}
