package player.test

import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerState
import player.common.TrackInfo
import player.common.VideoSize
import kotlin.time.Duration

class FakeAppPlayer(
    val fakeTracks: MutableList<TrackInfo> = mutableListOf()
) : AppPlayer {
    var releaseCount = 0
    val collectedEvents = mutableListOf<PlayerEvent>()

    override val state: PlayerState get() = PlayerState.INITIAL
    override val tracks: List<TrackInfo> get() = fakeTracks
    override val videoSize: VideoSize = VideoSize(0, 0)

    override fun onEvent(playerEvent: PlayerEvent) {
        collectedEvents += playerEvent
    }

    override fun play() {
        error("unused")
    }

    override fun pause() {
        error("unused")
    }

    override fun seekRelative(duration: Duration) {
        error("unused")
    }

    override fun seekTo(duration: Duration) {
        error("unused")
    }

    override fun toPlaylistItem(index: Int) {
        error("unused")
    }

    override fun hasMedia(): Boolean {
        error("unused")
    }

    override fun release() {
        ++releaseCount
    }

    override fun handlePlaybackInfos(playbackInfos: List<PlaybackInfo>) = Unit
    override fun clearTrackInfos(rendererIndex: Int) = Unit
    override fun setTrackInfos(trackInfos: List<TrackInfo>) = Unit
}
