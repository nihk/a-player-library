package player.mediaplayer

import android.media.MediaPlayer
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerState
import player.common.TrackInfo
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MediaPlayerWrapper(
    val mediaPlayer: MediaPlayer
) : AppPlayer {

    private var initialState: PlayerState? = null

    override val state: PlayerState
        get() {
            return PlayerState(
                positionMs = mediaPlayer.currentPosition.toLong(),
                isPlaying = mediaPlayer.isPlaying
            )
        }

    override val tracks: List<TrackInfo>
        get() = emptyList()

    override fun setPlayerState(playerState: PlayerState) {
        // Cache until MediaPlayer is ready to have this state set.
        initialState = playerState
    }

    override fun handleTrackInfoAction(action: TrackInfo.Action) {
        when (action) {
            is TrackInfo.Action.Clear -> mediaPlayer.clearTrackInfos(action.rendererIndex)
            is TrackInfo.Action.Set -> action.trackInfos.forEach(mediaPlayer::setTrackInfo)
        }
    }

    override fun onEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.OnPlayerPrepared -> initialState?.run {
                mediaPlayer.seekTo(positionMs.toInt())
                // MediaPlayer enforces calling start before pause
                mediaPlayer.start()
                if (!isPlaying) {
                    mediaPlayer.pause()
                }
            }.also { initialState = null }
        }
    }

    override fun play() {
        mediaPlayer.start()
    }

    override fun pause() {
        mediaPlayer.pause()
    }

    override fun seekRelative(duration: Duration) {
        val current = mediaPlayer.currentPosition.toDuration(DurationUnit.MILLISECONDS)
        val seekTo = current + duration
        seekTo(seekTo)
    }

    override fun seekTo(duration: Duration) {
        mediaPlayer.seekTo(duration.inWholeMilliseconds.toInt())
    }

    override fun release() {
        with(mediaPlayer) {
            stop()
            reset()
            release()
        }
    }

    class Factory : AppPlayer.Factory {
        override fun create(playbackInfo: PlaybackInfo): AppPlayer {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(playbackInfo.uri)
                prepareAsync()
            }
            return MediaPlayerWrapper(mediaPlayer)
        }
    }
}