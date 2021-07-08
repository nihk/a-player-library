package player.ui.test

import player.common.AspectRatio
import player.common.SeekData
import player.common.TrackInfo
import player.ui.common.PlayerController
import kotlin.time.Duration

class FakePlayerController : PlayerController {
    override fun play() = Unit
    override fun pause() = Unit
    override fun isPlaying(): Boolean = false
    override fun hasMedia(): Boolean = false
    override fun aspectRatio(): AspectRatio? = null
    override fun seekRelative(duration: Duration) = Unit
    override fun seekTo(duration: Duration) = Unit
    override fun toPlaylistItem(index: Int) = Unit
    override fun latestSeekData(): SeekData = SeekData.INITIAL
    override fun tracks(): List<TrackInfo> = emptyList()
}
