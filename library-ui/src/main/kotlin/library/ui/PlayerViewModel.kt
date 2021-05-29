package library.ui

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import library.common.AppPlayer
import library.common.SeekData
import library.common.PlaybackInfo
import library.common.PlaybackInfoResolver
import library.common.PlayerEvent
import library.common.PlayerEventStream
import library.common.PlayerState
import library.common.PlayerTelemetry
import library.common.PlayerViewWrapper
import library.common.SeekDataUpdater
import library.common.TrackInfo
import kotlin.time.Duration

class PlayerViewModel(
    private val playerSavedState: PlayerSavedState,
    private val appPlayerFactory: AppPlayer.Factory,
    private val playerEventStream: PlayerEventStream,
    private val telemetry: PlayerTelemetry?,
    private val playbackInfoResolver: PlaybackInfoResolver,
    uri: String,
    private val seekDataUpdater: SeekDataUpdater
) : ViewModel() {

    private var appPlayer: AppPlayer? = null
    private val playerJobs = mutableListOf<Job>()
    private val playbackInfo = CompletableDeferred<PlaybackInfo>()

    private val playerEvents = MutableSharedFlow<PlayerEvent>()
    fun playerEvents(): Flow<PlayerEvent> = playerEvents

    private val uiStates = MutableStateFlow(UiState.INITIAL)
    fun uiStates(): Flow<UiState> = uiStates

    private val errors = MutableSharedFlow<String>()
    fun errors(): Flow<String> = errors

    private val tracksStates = MutableStateFlow(TracksState.NotAvailable)
    fun tracksStates(): Flow<TracksState> = tracksStates

    init {
        viewModelScope.launch {
            try {
                // todo: investigate possibility of this happening as a flow of events rather
                //  than a one-shot
                val playbackInfo = playbackInfoResolver.resolve(uri)
                this@PlayerViewModel.playbackInfo.complete(playbackInfo)
            } catch (throwable: Throwable) {
                playbackInfo.completeExceptionally(throwable)
            }
            uiStates.value = uiStates.value.copy(isResolvingMedia = false)
        }
    }

    suspend fun bind(playerViewWrapper: PlayerViewWrapper) {
        if (appPlayer == null) {
            try {
                val playbackInfo = playbackInfo.await()
                appPlayer = appPlayerFactory.create(playbackInfo)
                playerJobs += listenToPlayerEvents(requireNotNull(appPlayer))
                playerJobs += seekDataUpdater.seekData(requireNotNull(appPlayer))
                    .onEach { opSeekData -> uiStates.value = uiStates.value.copy(seekData = opSeekData) }
                    .launchIn(viewModelScope)
                requireNotNull(appPlayer).bind(playerViewWrapper, playerSavedState.playerState() ?: PlayerState.INITIAL)
            } catch (throwable: Throwable) {
                errors.emit(throwable.message.toString())
            }
        } else {
            // This will get hit when the UI is going thru a config change; we don't need to set any
            // state here because the player is still active, in memory with up-to-date state.
            requireNotNull(appPlayer).bind(playerViewWrapper)
        }
    }

    fun unbind(playerViewWrapper: PlayerViewWrapper, isChangingConfigurations: Boolean) {
        playerViewWrapper.detach()

        if (isChangingConfigurations) {
            // We're only interested in saving state/tearing down the player when the app is backgrounded.
            return
        }

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
                        playerSavedState.manuallySetTracks().let { appPlayer.handleTrackInfoAction(TrackInfo.Action.Set(it)) }
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
    val isResolvingMedia: Boolean,
    val seekData: SeekData
) {
    companion object {
        val INITIAL = UiState(
            showController = false,
            isResolvingMedia = true,
            seekData = SeekData.INITIAL
        )
    }
}

enum class TracksState {
    Available,
    NotAvailable
}
