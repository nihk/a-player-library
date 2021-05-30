package player.ui

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlaybackInfoResolver
import player.common.PlayerEvent
import player.common.PlayerEventStream
import player.common.PlayerTelemetry
import player.common.SeekData
import player.common.SeekDataUpdater
import player.common.TrackInfo
import player.common.requireNotNull
import kotlin.time.Duration

class PlayerViewModel(
    private val playerSavedState: PlayerSavedState,
    private val appPlayerFactory: AppPlayer.Factory,
    private val playerEventStream: PlayerEventStream,
    private val telemetry: PlayerTelemetry?,
    playbackInfoResolver: PlaybackInfoResolver,
    uri: String,
    private val seekDataUpdater: SeekDataUpdater
) : ViewModel() {

    private var appPlayer: AppPlayer? = null
    private val playerJobs = mutableListOf<Job>()

    private val playerEvents = MutableSharedFlow<PlayerEvent>()
    fun playerEvents(): Flow<PlayerEvent> = playerEvents

    private val uiStates = MutableStateFlow(UiState.INITIAL)
    fun uiStates(): Flow<UiState> = uiStates

    private val errors = MutableSharedFlow<String>()
    fun errors(): Flow<String> = errors

    private val tracksStates = MutableStateFlow(TracksState.NotAvailable)
    fun tracksStates(): Flow<TracksState> = tracksStates

    val playbackInfos: StateFlow<List<PlaybackInfo>> = playbackInfoResolver.playbackInfos(uri)
        .onEach { playbackInfo ->
            if (playbackInfo is PlaybackInfo.MediaUri) {
                uiStates.value = uiStates.value.copy(showLoading = false)
            }
        }
        .runningFold(emptyList<PlaybackInfo>()) { list, playbackInfo ->
            list + listOf(playbackInfo)
        }
        .onEach { playbackInfos -> appPlayer?.handlePlaybackInfos(playbackInfos) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun getPlayer(): AppPlayer {
        if (appPlayer == null) {
            appPlayer = appPlayerFactory.create(playerSavedState.playerState())
            val appPlayer = appPlayer.requireNotNull()
            appPlayer.handlePlaybackInfos(playbackInfos.value)
            playerJobs += listenToPlayerEvents(appPlayer)
            playerJobs += seekDataUpdater.seekData(appPlayer)
                .onEach { opSeekData -> uiStates.value = uiStates.value.copy(seekData = opSeekData) }
                .launchIn(viewModelScope)
        }

        return appPlayer.requireNotNull()
    }

    fun onAppBackgrounded() {
        // When a user backgrounds the app, then later foregrounds it back to the video, a good UX is
        // to have the player be paused upon return.
        val state = appPlayer?.state?.copy(isPlaying = false)
        playerSavedState.save(state, appPlayer?.tracks.orEmpty())
        tearDown()
    }

    private fun listenToPlayerEvents(appPlayer: AppPlayer): Job {
        return playerEventStream.listen(appPlayer)
            .onEach { playerEvent -> appPlayer.onEvent(playerEvent) }
            .onEach { playerEvent -> playerEvents.emit(playerEvent) }
            .onEach { playerEvent -> telemetry?.onPlayerEvent(playerEvent) }
            .onEach { playerEvent ->
                when (playerEvent) {
                    is PlayerEvent.Initial -> uiStates.value = uiStates.value.copy(showController = true)
                    is PlayerEvent.OnTracksAvailable -> {
                        val action = TrackInfo.Action.Set(playerSavedState.manuallySetTracks())
                        appPlayer.handleTrackInfoAction(action)
                        tracksStates.value = TracksState.Available
                    }
                    is PlayerEvent.OnPlayerError -> errors.emit(playerEvent.exception.message.toString())
                }
            }
            .launchIn(viewModelScope)
    }

    fun isPlaying(): Boolean = appPlayer?.state?.isPlaying == true

    fun play() {
        requireNotNull(appPlayer).play()
    }

    fun pause() {
        requireNotNull(appPlayer).pause()
    }

    override fun onCleared() {
        tearDown()
    }

    private fun tearDown() {
        tracksStates.value = TracksState.NotAvailable
        playerJobs.forEach(Job::cancel)
        playerJobs.clear()
        // This can be nullable when closing PiP. PiP can recreate/destroy the Activity without
        // creating an AppPlayer instance in this ViewModel.
        appPlayer?.release()
        appPlayer = null
    }

    fun tracks(): List<TrackInfo> {
        return requireNotNull(appPlayer).tracks
    }

    fun handleTrackInfoAction(action: TrackInfo.Action) {
        requireNotNull(appPlayer).handleTrackInfoAction(action)
    }

    fun onPipModeChanged(isInPipMode: Boolean) {
        uiStates.value = uiStates.value.copy(showController = !isInPipMode)
    }

    fun seekRelative(duration: Duration) {
        requireNotNull(appPlayer).seekRelative(duration)
    }

    fun seekTo(duration: Duration) {
        requireNotNull(appPlayer).seekTo(duration)
    }

    class Factory(
        private val appPlayerFactory: AppPlayer.Factory,
        private val playerEventStream: PlayerEventStream,
        private val telemetry: PlayerTelemetry?,
        private val playbackInfoResolver: PlaybackInfoResolver,
        private val seekDataUpdater: SeekDataUpdater
    ) {
        fun create(
            owner: SavedStateRegistryOwner,
            uri: String
        ): AbstractSavedStateViewModelFactory {
            return object : AbstractSavedStateViewModelFactory(owner, null) {
                override fun <T : ViewModel?> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return PlayerViewModel(
                        PlayerSavedState(handle),
                        appPlayerFactory,
                        playerEventStream,
                        telemetry,
                        playbackInfoResolver,
                        uri,
                        seekDataUpdater
                    ) as T
                }
            }
        }
    }
}

data class UiState(
    val showController: Boolean,
    val showLoading: Boolean,
    val seekData: SeekData
) {
    companion object {
        val INITIAL = UiState(
            showController = false,
            showLoading = true,
            seekData = SeekData.INITIAL
        )
    }
}

// todo: what if tracks change due to late PlaybackInfo resolving, e.g. text tracks
enum class TracksState {
    Available,
    NotAvailable
}
