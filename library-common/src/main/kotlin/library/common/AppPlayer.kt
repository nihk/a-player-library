package library.common

interface AppPlayer {
    val state: PlayerState
    // fixme: can these just be exposed via `state`?
    val textTracks: List<TrackInfo>
    val audioTracks: List<TrackInfo>
    val videoTracks: List<TrackInfo>

    fun bind(playerViewWrapper: PlayerViewWrapper, playerState: PlayerState? = null)
    fun handleTrackInfoAction(action: TrackInfo.Action)
    fun play()
    fun pause()
    fun release()

    fun onEvent(playerEvent: PlayerEvent) = Unit

    interface Factory {
        fun create(uri: String): AppPlayer
    }
}