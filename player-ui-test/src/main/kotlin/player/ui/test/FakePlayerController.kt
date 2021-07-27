package player.ui.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import player.common.PlayerEvent
import player.common.SeekData
import player.common.TrackInfo
import player.common.VideoSize
import player.ui.common.PlayerController
import player.ui.common.UiState
import kotlin.time.Duration

class FakePlayerController(private var isPlaying: Boolean = false) : PlayerController {
    override fun play() = Unit
    override fun pause() = Unit
    override fun isPlaying(): Boolean = isPlaying
    fun setIsPlaying(isPlaying: Boolean) {
        this.isPlaying = isPlaying
    }
    override fun hasMedia(): Boolean = false
    override fun videoSize(): VideoSize? = null
    override fun seekRelative(duration: Duration) = Unit
    override fun seekTo(duration: Duration) = Unit
    override fun toPlaylistItem(index: Int) = Unit
    override fun latestSeekData(): SeekData = SeekData.INITIAL
    override fun tracks(): List<TrackInfo> = emptyList()
    override fun clearTrackInfos(rendererIndex: Int) = Unit
    override fun setTrackInfos(trackInfos: List<TrackInfo>) = Unit
    override fun playerEvents(): Flow<PlayerEvent> = emptyFlow()
    override fun uiStates(): StateFlow<UiState> = MutableStateFlow(UiState.INITIAL)
}
