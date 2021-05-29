package library.common

import kotlin.time.Duration

interface AppPlayer {
    val state: PlayerState
    val tracks: List<TrackInfo>

    fun bind(playerViewWrapper: PlayerViewWrapper, playerState: PlayerState? = null)
    fun handleTrackInfoAction(action: TrackInfo.Action)
    fun play()
    fun pause()
    fun seekRelative(duration: Duration)
    fun seekTo(duration: Duration)
    fun release()

    fun onEvent(playerEvent: PlayerEvent) = Unit

    interface Factory {
        fun create(playbackInfo: PlaybackInfo): AppPlayer
    }
}