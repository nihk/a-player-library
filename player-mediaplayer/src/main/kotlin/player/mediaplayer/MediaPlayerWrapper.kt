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
    val mediaPlayer: MediaPlayer,
    private val initial: PlayerState
) : AppPlayer {
    private var didSetMediaSource = false

    override val state: PlayerState
        get() {
            return PlayerState(
                itemIndex = 0, // todo
                positionMillis = mediaPlayer.currentPosition.toLong(),
                isPlaying = mediaPlayer.isPlaying
            )
        }

    override val tracks: List<TrackInfo>
        get() = emptyList()

    override fun handlePlaybackInfos(playbackInfos: List<PlaybackInfo>) {
        playbackInfos.forEach { playbackInfo ->
            when (playbackInfo) {
                is PlaybackInfo.MediaUri -> {
                    if (!didSetMediaSource) {
                        mediaPlayer.setDataSource(playbackInfo.uri)
                        mediaPlayer.prepareAsync()
                        didSetMediaSource = true
                    }
                }
            }
        }
    }

    override fun handleTrackInfoAction(action: TrackInfo.Action) {
        when (action) {
            is TrackInfo.Action.Clear -> mediaPlayer.clearTrackInfos(action.rendererIndex)
            is TrackInfo.Action.Set -> action.trackInfos.forEach(mediaPlayer::setTrackInfo)
        }
    }

    override fun onEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.OnPlayerPrepared -> {
                mediaPlayer.seekTo(initial.positionMillis.toInt())
                // MediaPlayer enforces calling start before pause
                mediaPlayer.start()
                if (!initial.isPlaying) {
                    mediaPlayer.pause()
                }
            }
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

    override fun toPlaylistItem(index: Int) {
        // todo
    }

    override fun release() {
        with(mediaPlayer) {
            stop()
            reset()
            release()
        }
    }

    class Factory : AppPlayer.Factory {
        override fun create(initial: PlayerState): AppPlayer {
            return MediaPlayerWrapper(MediaPlayer(), initial)
        }
    }
}