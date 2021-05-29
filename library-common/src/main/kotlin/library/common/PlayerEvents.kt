package library.common

import kotlinx.coroutines.flow.Flow

sealed class PlayerEvent {
    data class Initial(val playerState: PlayerState) : PlayerEvent()
    // todo: need to make every event be relevant for both exoplayer/mediaplayer
    object OnPlayerPrepared : PlayerEvent()
    data class OnInfo(val what: Int, val extra: Int) : PlayerEvent()
    data class OnVideoSizeChanged(val width: Int, val height: Int) : PlayerEvent()
    data class OnIsLoadingChanged(val isLoading: Boolean) : PlayerEvent()
    data class OnPlayWhenReadyChanged(val playWhenReady: Boolean, val reason: Int) : PlayerEvent()
    data class OnPlaybackStateChanged(val state: Int) : PlayerEvent()
    object OnTracksAvailable : PlayerEvent()
    object OnTracksChanged : PlayerEvent()
    data class OnPlayerError(val exception: PlayerException) : PlayerEvent()
    data class OnIsPlayingChanged(val isPlaying: Boolean) : PlayerEvent()
}

interface PlayerEventStream {
    fun listen(appPlayer: AppPlayer): Flow<PlayerEvent>
}