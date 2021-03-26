package library.common

interface AppPlayer {
    val state: PlayerState
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
        fun create(url: String): AppPlayer
    }
}