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
import player.test.FakeAppPlayer
import player.test.FakeAppPlayerFactory
import player.test.FakePlayerEventDelegate
import player.test.FakePlayerEventStream
import player.test.FakeSeekDataUpdater
import player.ui.common.TracksState

class PlayerNonConfigTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun `getPlayer creates a new player when there is no active player`() = playerNonConfig {
        getPlayer()
        assertPlayerCreated(times = 1)
    }

    @Test
    fun `multiple getPlayer calls only create player once`() = playerNonConfig {
        getPlayer()
        getPlayer()
        assertPlayerCreated(times = 1)
    }

    @Test
    fun `player state is used when creating new player`() = playerNonConfig {
        val playerState = PlayerState(itemIndex = 0, positionMillis = 5000L, isPlaying = false)
        setPlayerState(playerState)
        getPlayer()
        assertPlayerCreatedWithState(playerState)
    }

    @Test
    fun `player events can be received when player is created`() = playerNonConfig {
        getPlayer()
        emit(PlayerEvent.Initial)
        assertEmission(PlayerEvent.Initial)
    }

    @Test
    fun `player events are not received if player is not created`() = playerNonConfig {
        emit(PlayerEvent.Initial)
        assertNoEmission(PlayerEvent.Initial)
    }

    @Test
    fun `player events stop being received after app is backgrounded`() = playerNonConfig {
        getPlayer()
        emit(PlayerEvent.Initial)
        onAppBackgrounded()
        val tracksChanged = PlayerEvent.OnTracksChanged(emptyList())
        emit(tracksChanged)
        assertNoEmission(tracksChanged)
    }

    @Test
    fun `app player receives player events`() = playerNonConfig {
        getPlayer()
        emit(PlayerEvent.Initial)
        assertAppPlayerEmission(PlayerEvent.Initial)
    }

    @Test
    fun `delegate receives player events`() = playerNonConfig {
        getPlayer()
        emit(PlayerEvent.Initial)
        assertPlayerEventEmission(PlayerEvent.Initial)
    }

    @Test
    fun `player did tear down when app is backgrounded`() = playerNonConfig {
        getPlayer()
        onAppBackgrounded()
        assertReleased(times = 1)
    }

    @Test
    fun `track state changes according to relevant player event emissions`() = playerNonConfig {
        getPlayer()
        assertEmission(TracksState.NotAvailable)
        val event = PlayerEvent.OnTracksChanged(emptyList())
        emit(event)
        assertEmission(event)
    }

    private fun playerNonConfig(block: suspend PlayerNonConfigRobot.() -> Unit) = coroutinesTestRule.testDispatcher.runBlockingTest {
        PlayerNonConfigRobot(this)
            .apply { block() }
            .release()
    }

    private class PlayerNonConfigRobot(scope: CoroutineScope) {
        private val playerSavedState = PlayerSavedState("player_saved_state", SavedStateHandle())
        private val appPlayer = FakeAppPlayer()
        private val appPlayerFactory = FakeAppPlayerFactory(appPlayer)
        private val events = MutableSharedFlow<PlayerEvent>()
        private val playerEventStream = FakePlayerEventStream(events)
        private val playerEventDelegate = FakePlayerEventDelegate()
        private val playbackInfoResolver = DefaultPlaybackInfoResolver()
        private val seekDataUpdater = FakeSeekDataUpdater()
        private val playerNonConfig = PlayerNonConfig(
            playerSavedState = playerSavedState,
            appPlayerFactory = appPlayerFactory,
            playerEventStream = playerEventStream,
            playerEventDelegate = playerEventDelegate,
            playbackInfoResolver = playbackInfoResolver,
            uri = "https://www.example.com/video.mp4",
            seekDataUpdater = seekDataUpdater
        )
        private val emittedEvents = mutableListOf<PlayerEvent>()
        private val emittedTrackStates = mutableListOf<TracksState>()
        private val jobs = mutableListOf<Job>()

        init {
            jobs += playerNonConfig.playerEvents()
                .onEach { emittedEvents += it }
                .launchIn(scope)

            jobs += playerNonConfig.tracksStates()
                .onEach { emittedTrackStates += it }
                .launchIn(scope)
        }

        fun release() {
            jobs.forEach(Job::cancel)
            playerNonConfig.close()
        }

        suspend fun emit(playerEvent: PlayerEvent = PlayerEvent.Initial) {
            events.emit(playerEvent)
        }

        fun setPlayerState(playerState: PlayerState?) {
            playerSavedState.save(playerState, emptyList())
        }

        fun getPlayer() {
            playerNonConfig.getPlayer()
        }

        fun onAppBackgrounded() {
            playerNonConfig.onAppBackgrounded()
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

        fun assertPlayerEventEmission(playerEvent: PlayerEvent) {
            assertTrue(playerEvent in playerEventDelegate.collectedEvents)
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
