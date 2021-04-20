package library.mediaplayer

import android.media.MediaPlayer
import library.common.AppPlayer
import library.common.PlayerEvent
import library.common.PlayerState
import library.common.PlayerViewWrapper
import library.common.TrackInfo

class MediaPlayerWrapper(
    val mediaPlayer: MediaPlayer
) : AppPlayer {

    private var initialState: PlayerState? = null

    override val state: PlayerState
        get() {
            return PlayerState(
                positionMs = mediaPlayer.currentPosition.toLong(),
                isPlaying = mediaPlayer.isPlaying,
                trackInfos = (textTracks + audioTracks + videoTracks).filter(TrackInfo::isManuallySet)
            )
        }

    override val textTracks: List<TrackInfo>
        get() = mediaPlayer.getTrackInfos(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE)

    override val audioTracks: List<TrackInfo>
        get() = mediaPlayer.getTrackInfos(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO)

    override val videoTracks: List<TrackInfo>
        get() = mediaPlayer.getTrackInfos(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO)

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

    override fun release() {
        with(mediaPlayer) {
            stop()
            reset()
            release()
        }
    }

    class Factory : AppPlayer.Factory {
        override fun create(uri: String): AppPlayer {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(uri)
                prepareAsync()
            }
            return MediaPlayerWrapper(mediaPlayer)
        }
    }
}