package player.common

import kotlinx.coroutines.flow.Flow

sealed class PlayerEvent {
    object Initial : PlayerEvent()
    // todo: need to make every event be relevant for both exoplayer/mediaplayer
    data class OnPlayerPrepared(val playWhenReady: Boolean) : PlayerEvent()
    data class OnInfo(val what: Int, val extra: Int) : PlayerEvent()
    data class OnVideoSizeChanged(val width: Int, val height: Int) : PlayerEvent()
    data class OnIsLoadingChanged(val isLoading: Boolean) : PlayerEvent()
    data class OnPlayWhenReadyChanged(val playWhenReady: Boolean, val reason: Int) : PlayerEvent()
    data class OnPlaybackStateChanged(val state: PlaybackState) : PlayerEvent()
    data class OnTracksChanged(val trackInfos: List<TrackInfo>) : PlayerEvent()
    data class OnPlayerError(val exception: PlayerException) : PlayerEvent()
    data class OnIsPlayingChanged(val isPlaying: Boolean) : PlayerEvent()
}

interface PlayerEventStream {
    fun listen(appPlayer: AppPlayer): Flow<PlayerEvent>
}