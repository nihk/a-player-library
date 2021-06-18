package player.common

import kotlin.time.Duration

interface AppPlayer {
    val state: PlayerState
    val tracks: List<TrackInfo>
    val aspectRatio: AspectRatio

    fun handlePlaybackInfos(playbackInfos: List<PlaybackInfo>)
    fun handleTrackInfoAction(action: TrackInfo.Action)
    fun play()
    fun pause()
    fun seekRelative(duration: Duration)
    fun seekTo(duration: Duration)
    fun toPlaylistItem(index: Int)
    fun hasMedia(): Boolean
    fun release()

    fun onEvent(playerEvent: PlayerEvent) = Unit

    interface Factory {
        fun create(initial: PlayerState): AppPlayer
    }
}