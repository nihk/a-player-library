package player.ui.common

import player.common.SeekData
import player.common.TrackInfo
import kotlin.time.Duration

// todo: consider merging with AppPlayer
interface PlayerController {
    fun play()
    fun pause()
    fun isPlaying(): Boolean
    fun seekRelative(duration: Duration)
    fun seekTo(duration: Duration)
    fun toPlaylistItem(index: Int)
    fun latestSeekData(): SeekData
    fun tracks(): List<TrackInfo>
}