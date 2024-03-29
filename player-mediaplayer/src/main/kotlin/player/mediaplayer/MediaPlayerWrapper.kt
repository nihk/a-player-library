package player.mediaplayer

import android.media.MediaPlayer
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerState
import player.common.TrackInfo
import player.common.VideoSize
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

    override val videoSize: VideoSize
        get() = VideoSize(0, 0) // todo

    override fun handlePlaybackInfos(playbackInfos: List<PlaybackInfo>) {
        playbackInfos.forEach { playbackInfo ->
            when (playbackInfo) {
                is PlaybackInfo.RelatedMedia, is PlaybackInfo.MediaUri -> {
                    val relatedMedia = playbackInfo as? PlaybackInfo.RelatedMedia
                    val mediaUri = playbackInfo as? PlaybackInfo.MediaUri
                    val uri = relatedMedia?.uri ?: mediaUri?.uri ?: error("This should never happen")

                    if (!didSetMediaSource) {
                        mediaPlayer.setDataSource(uri)
                        mediaPlayer.prepareAsync()
                        didSetMediaSource = true
                    }
                }
            }
        }
    }

    override fun setTrackInfos(trackInfos: List<TrackInfo>) {
        mediaPlayer.setTrackInfos(trackInfos)
    }

    override fun clearTrackInfos(rendererIndex: Int) {
        mediaPlayer.clearTrackInfos(rendererIndex)
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

    override fun hasMedia(): Boolean {
        return didSetMediaSource
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