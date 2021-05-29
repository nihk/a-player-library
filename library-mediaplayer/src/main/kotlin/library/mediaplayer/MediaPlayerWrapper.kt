package library.mediaplayer

import android.media.MediaPlayer
import library.common.AppPlayer
import library.common.PlayerEvent
import library.common.PlayerState
import library.common.PlayerViewWrapper
import library.common.PlaybackInfo
import library.common.TrackInfo
import kotlin.time.Duration

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

    override fun bind(playerViewWrapper: PlayerViewWrapper, playerState: PlayerState?) {
        // Cache until MediaPlayer is ready to have this state set.
        initialState = playerState
        playerViewWrapper.attachTo(this)
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
        // todo
    }

    override fun seekTo(duration: Duration) {
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
        override fun create(playbackInfo: PlaybackInfo): AppPlayer {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(playbackInfo.uri)
                prepareAsync()
            }
            return MediaPlayerWrapper(mediaPlayer)
        }
    }
}