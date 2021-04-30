package library.ui

import library.CoroutinesTestRule
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runBlockingTest
import library.common.PlayerEvent
import library.common.PlayerState
import library.test.NoOpPlayerViewWrapper
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlayerViewModelTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun `bind creates a new player when there is no active player`() = playerViewModel {
        bind()
        assertBindedToPlayerView()
        assertPlayerCreated(times = 1)
    }

    @Test
    fun `player state is used when binding new player`() = playerViewModel {
        val playerState = PlayerState(positionMs = 5000L, isPlaying = false, trackInfos = emptyList())
        setPlayerState(playerState)
        bind()
        assertPlayerBindedWithState(playerState)
    }

    @Test
    fun `player events can be received when player is binded to`() = playerViewModel {
        bind()
        emit(PlayerEvent.OnTracksAvailable)
        assertEmission(PlayerEvent.OnTracksAvailable)
    }

    @Test
    fun `player events are not received if player is not binded to`() = playerViewModel {
        emit(PlayerEvent.OnTracksAvailable)
        assertNoEmission(PlayerEvent.OnTracksAvailable)
    }

    @Test
    fun `player events stop being received after player is unbinded from`() = playerViewModel {
        bind()
        emit(PlayerEvent.OnTracksAvailable)
        unbind()
        emit(PlayerEvent.OnPlayerPrepared)
        assertNoEmission(PlayerEvent.OnPlayerPrepared)
    }

    @Test
    fun `app player receives player events`() = playerViewModel {
        bind()
        emit(PlayerEvent.OnTracksAvailable)
        assertAppPlayerEmission(PlayerEvent.OnTracksAvailable)
    }

    @Test
    fun `telemetry receives player events`() = playerViewModel {
        bind()
        emit(PlayerEvent.OnTracksAvailable)
        assertTelemetryEmission(PlayerEvent.OnTracksAvailable)
    }

    @Test
    fun `player is not torn down when unbinded across configuration changes`() = playerViewModel {
        bind()
        unbind(isChangingConfigurations = true)
        assertNotReleased()
    }

    @Test
    fun `player did tear down when unbinded without changing configuration`() = playerViewModel {
        bind()
        unbind(isChangingConfigurations = false)
        assertReleased()
    }

    @Test
    fun `player view is detached on unbind - during config change`() = playerViewModel {
        bind()
        unbind(isChangingConfigurations = true)
        assertDetachedView()
    }

    @Test
    fun `player view is detached on unbind - no config change`() = playerViewModel {
        bind()
        unbind(isChangingConfigurations = false)
        assertDetachedView()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calling unbind before binding a player, throws`() = playerViewModel {
        unbind()
        fail("Expected IAE was not thrown")
    }

    @Test
    fun `track state changes according to relevant player event emissions`() = playerViewModel {
        bind()
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
    private val playerViewWrapper = NoOpPlayerViewWrapper()
    private val viewModel = PlayerViewModel(
        playerSavedState = playerSavedState,
        appPlayerFactory = appPlayerFactory,
        playerEventStream = playerEventStream,
        telemetry = telemetry
    )
    private val emittedEvents = mutableListOf<PlayerEvent>()
    private val emittedTrackStates = mutableListOf<TracksState>()
    private val jobs = mutableListOf<Job>()

    init {
        jobs += viewModel.playerEvents()
            .onEach { emittedEvents += it }
            .launchIn(scope)

        jobs += viewModel.uiStates()
            .onEach { emittedTrackStates += it.tracksState }
            .launchIn(scope)
    }

    fun release() {
        jobs.forEach(Job::cancel)
    }

    suspend fun emit(playerEvent: PlayerEvent = PlayerEvent.OnTracksAvailable) {
        events.emit(playerEvent)
    }

    fun setPlayerState(playerState: PlayerState?) {
        playerSavedState.value = playerState
    }

    fun bind() {
        viewModel.bind(playerViewWrapper, "https://www.example.com")
    }

    fun unbind(isChangingConfigurations: Boolean = false) {
        viewModel.unbind(playerViewWrapper, isChangingConfigurations)
    }

    fun assertPlayerCreated(times: Int) {
        assertEquals(times, appPlayerFactory.createCount)
    }

    fun assertPlayerBindedWithState(playerState: PlayerState?) {
        assertEquals(playerState, appPlayer.boundState)
    }

    fun assertBindedToPlayerView() {
        assertEquals(playerViewWrapper, appPlayer.boundPlayerViewWrapper)
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

    fun assertDetachedView() {
        assertTrue(playerViewWrapper.didDetach)
    }

    fun assertReleased() {
        assertTrue(appPlayer.didRelease)
    }

    fun assertNotReleased() {
        assertFalse(appPlayer.didRelease)
    }
}