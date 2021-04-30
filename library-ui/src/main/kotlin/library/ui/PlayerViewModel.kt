package library.ui

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import library.common.AppPlayer
import library.common.PlayerEvent
import library.common.PlayerEventStream
import library.common.PlayerState
import library.common.PlayerTelemetry
import library.common.PlayerViewWrapper
import library.common.TrackInfo

class PlayerViewModel(
    private val playerSavedState: PlayerSavedState,
    private val appPlayerFactory: AppPlayer.Factory,
    private val playerEventStream: PlayerEventStream,
    private val telemetry: PlayerTelemetry?
) : ViewModel() {

    private var appPlayer: AppPlayer? = null
    private var listening: Job? = null

    private val playerEvents = MutableSharedFlow<PlayerEvent>()
    fun playerEvents(): Flow<PlayerEvent> = playerEvents

    private val uiStates = MutableStateFlow(UiState.INITIAL)
    fun uiStates(): Flow<UiState> = uiStates

    private val errors = MutableSharedFlow<String>()
    fun errors(): Flow<String> = errors

    fun bind(playerViewWrapper: PlayerViewWrapper, uri: String) {
        if (appPlayer == null) {
            appPlayer = appPlayerFactory.create(uri)
            listening = listenToPlayerEvents(requireNotNull(appPlayer))
            requireNotNull(appPlayer).bind(playerViewWrapper, playerSavedState.value ?: PlayerState.INITIAL)
        } else {
            // This will get hit when the UI is going thru a config change; we don't need to set any
            // state here because the player is still active, in memory with up-to-date state.
            requireNotNull(appPlayer).bind(playerViewWrapper)
        }
    }

    fun unbind(playerViewWrapper: PlayerViewWrapper, isChangingConfigurations: Boolean) {
        val appPlayer = requireNotNull(appPlayer)
        playerViewWrapper.detach()

        if (isChangingConfigurations) {
            // We're only interested in saving state/tearing down the player when the app is backgrounded.
            return
        }

        // When a user backgrounds the app, then later foregrounds it back to the video, a good UX is
        // to have the player be paused upon return.
        playerSavedState.value = appPlayer.state.copy(isPlaying = false)
        tearDown()
    }

    private fun listenToPlayerEvents(appPlayer: AppPlayer): Job {
        return playerEventStream.listen(appPlayer)
            .onEach { playerEvent -> appPlayer.onEvent(playerEvent) }
            .onEach { playerEvent -> playerEvents.emit(playerEvent) }
            .onEach { playerEvent -> telemetry?.onPlayerEvent(playerEvent) }
            .onEach { playerEvent ->
                when (playerEvent) {
                    is PlayerEvent.OnTracksAvailable -> {
                        playerSavedState.value?.trackInfos?.let { appPlayer.handleTrackInfoAction(TrackInfo.Action.Set(it)) }
                        uiStates.value = uiStates.value.copy(tracksState = TracksState.Available)
                    }
                    is PlayerEvent.OnPlayerError -> errors.emit(playerEvent.exception.message.toString())
                }
            }
            .launchIn(viewModelScope)
    }

    fun play() {
        requireNotNull(appPlayer).play()
    }

    fun isPlaying(): Boolean {
        return requireNotNull(appPlayer).state.isPlaying
    }

    fun pause() {
        requireNotNull(appPlayer).pause()
    }

    override fun onCleared() {
        tearDown()
    }

    private fun tearDown() {
        uiStates.value = uiStates.value.copy(tracksState = TracksState.NotAvailable)
        // These can be nullable when closing PiP. PiP can recreate/destroy the Activity without
        // creating an AppPlayer instance in this ViewModel.
        listening?.cancel()
        listening = null
        appPlayer?.release()
        appPlayer = null
    }

    fun textTracks(): List<TrackInfo> {
        return requireNotNull(appPlayer).textTracks
    }

    fun audioTracks(): List<TrackInfo> {
        return requireNotNull(appPlayer).audioTracks
    }

    fun videoTracks(): List<TrackInfo> {
        return requireNotNull(appPlayer).videoTracks
    }

    fun handleTrackInfoAction(action: TrackInfo.Action) {
        requireNotNull(appPlayer).handleTrackInfoAction(action)
    }

    fun onPipModeChanged(isInPipMode: Boolean) {
        uiStates.value = uiStates.value.copy(useController = !isInPipMode)
    }

    class Factory(
        private val appPlayerFactory: AppPlayer.Factory,
        private val playerEventStream: PlayerEventStream,
        private val telemetry: PlayerTelemetry?
    ) {
        fun create(owner: SavedStateRegistryOwner): AbstractSavedStateViewModelFactory {
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
                        telemetry
                    ) as T
                }
            }
        }
    }
}

data class UiState(
    val tracksState: TracksState,
    val useController: Boolean
) {
    companion object {
        val INITIAL = UiState(
            tracksState = TracksState.NotAvailable,
            useController = true
        )
    }
}

enum class TracksState {
    Available,
    NotAvailable
}