package library.exoplayer

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.TrackNameProvider
import library.common.AppPlayer
import library.common.PlayerState
import library.common.PlayerViewWrapper
import library.common.PlaybackInfo
import library.common.TrackInfo

internal class ExoPlayerWrapper(
    internal val player: Player,
    private val trackNameProvider: TrackNameProvider
) : AppPlayer {

    init {
        player.prepare()
    }

    override val state: PlayerState
        get() {
            return PlayerState(
                positionMs = player.currentPosition,
                isPlaying = player.isPlaying,
                trackInfos = tracks.filter(TrackInfo::isManuallySet)
            )
        }

    override val tracks: List<TrackInfo>
        get() = player.getTrackInfos(listOf(C.TRACK_TYPE_TEXT, C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_VIDEO), trackNameProvider)

    override fun bind(playerViewWrapper: PlayerViewWrapper, playerState: PlayerState?) {
        playerState?.run {
            player.seekTo(positionMs)
            player.playWhenReady = isPlaying
        }
        playerViewWrapper.attachTo(this)
    }

    override fun handleTrackInfoAction(action: TrackInfo.Action) {
        when (action) {
            is TrackInfo.Action.Clear -> player.clearTrackOverrides(action.rendererIndex)
            is TrackInfo.Action.Set -> action.trackInfos.forEach(player::setTrackInfo)
        }
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun release() {
        player.release()
    }

    class Factory(
        private val appContext: Context,
        private val trackNameProvider: TrackNameProvider
    ) : AppPlayer.Factory {
        override fun create(playbackInfo: PlaybackInfo): AppPlayer {
            val player = SimpleExoPlayer.Builder(appContext)
                .build()
                .apply { setMediaItem(MediaItem.fromUri(playbackInfo.uri)) }
            return ExoPlayerWrapper(player, trackNameProvider)
        }
    }
}