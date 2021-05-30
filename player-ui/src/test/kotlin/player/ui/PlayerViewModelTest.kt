package player.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runBlockingTest
import player.CoroutinesTestRule
import player.common.PlayerEvent
import player.common.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
        val playerState = PlayerState(positionMs = 5000L, isPlaying = false)
        setPlayerState(playerState)
        getPlayer()
        assertPlayerBindedWithState(playerState)
    }

    @Test
    fun `player events can be received when player is created`() = playerViewModel {
        getPlayer()
        emit(PlayerEvent.OnTracksAvailable)
        assertEmission(PlayerEvent.OnTracksAvailable)
    }

    @Test
    fun `player events are not received if player is not created`() = playerViewModel {
        emit(PlayerEvent.OnTracksAvailable)
        assertNoEmission(PlayerEvent.OnTracksAvailable)
    }

    @Test
    fun `player events stop being received after app is backgrounded`() = playerViewModel {
        getPlayer()
        emit(PlayerEvent.OnTracksAvailable)
        onAppBackgrounded()
        emit(PlayerEvent.OnPlayerPrepared)
        assertNoEmission(PlayerEvent.OnPlayerPrepared)
    }

    @Test
    fun `app player receives player events`() = playerViewModel {
        getPlayer()
        emit(PlayerEvent.OnTracksAvailable)
        assertAppPlayerEmission(PlayerEvent.OnTracksAvailable)
    }

    @Test
    fun `telemetry receives player events`() = playerViewModel {
        getPlayer()
        emit(PlayerEvent.OnTracksAvailable)
        assertTelemetryEmission(PlayerEvent.OnTracksAvailable)
    }

    @Test
    fun `player did tear down when app is backgrounded`() = playerViewModel {
        getPlayer()
        onAppBackgrounded()
        assertReleased()
    }

    @Test
    fun `track state changes according to relevant player event emissions`() = playerViewModel {
        getPlayer()
        assertEmission(TracksState.NotAvailable)
        assertNoEmission(TracksState.Available)
        emit(PlayerEvent.OnTracksAvailable)
        assertEmission(TracksState.Available)
    }
}

fun playerViewModel(block: suspend PlayerViewModelRobot.() -> Unit) = runBlockingTest {
    PlayerViewModelRobot(this)
        .apply { block() }
        .release()
}

class PlayerViewModelRobot(scope: CoroutineScope) {
    private val playerSavedState = PlayerSavedState(SavedStateHandle())
    private val appPlayer = FakeAppPlayer()
    private val appPlayerFactory = FakeAppPlayerFactory(appPlayer)
    private val events = MutableSharedFlow<PlayerEvent>()
    private val playerEventStream = FakePlayerEventStream(events)
    private val telemetry = FakePlayerTelemetry()
    private val playbackInfoResolver = NoOpPlaybackInfoResolver()
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

    suspend fun emit(playerEvent: PlayerEvent = PlayerEvent.OnTracksAvailable) {
        events.emit(playerEvent)
    }

    fun setPlayerState(playerState: PlayerState?) {
        playerSavedState.save(playerState, emptyList())
    }

    suspend fun getPlayer() {
        viewModel.getPlayer()
    }

    fun onAppBackgrounded() {
        viewModel.onAppBackgrounded()
    }

    fun assertPlayerCreated(times: Int) {
        assertEquals(times, appPlayerFactory.createCount)
    }

    fun assertPlayerBindedWithState(playerState: PlayerState?) {
        assertEquals(playerState, appPlayer.boundState)
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

    fun assertNoEmission(tracksState: TracksState) {
        assertFalse(tracksState in emittedTrackStates)
    }

    fun assertReleased() {
        assertTrue(appPlayer.didRelease)
    }

    fun assertNotReleased() {
        assertFalse(appPlayer.didRelease)
    }
}