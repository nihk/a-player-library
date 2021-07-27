package player.ui.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import player.common.PlayerEvent
import player.common.SeekData
import player.common.TrackInfo
import player.common.VideoSize
import kotlin.time.Duration

// todo: consider merging with AppPlayer
interface PlayerController {
    fun play()
    fun pause()
    fun isPlaying(): Boolean
    fun hasMedia(): Boolean
    fun videoSize(): VideoSize?
    fun seekRelative(duration: Duration)
    fun seekTo(duration: Duration)
    fun toPlaylistItem(index: Int)
    fun latestSeekData(): SeekData
    fun tracks(): List<TrackInfo>
    fun clearTrackInfos(rendererIndex: Int)
    fun setTrackInfos(trackInfos: List<TrackInfo>)
    fun playerEvents(): Flow<PlayerEvent>
    fun uiStates(): StateFlow<UiState>
}
