package player.exoplayer

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.TrackNameProvider
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerState
import player.common.TrackInfo
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class ExoPlayerWrapper(
    internal val player: ExoPlayer,
    private val trackNameProvider: TrackNameProvider,
    private val initial: PlayerState
) : AppPlayer {

    override val state: PlayerState
        get() {
            return PlayerState(
                positionMillis = player.currentPosition,
                isPlaying = player.isPlaying
            )
        }

    override val tracks: List<TrackInfo>
        get() = player.getTrackInfos(trackNameProvider, C.TRACK_TYPE_TEXT, C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_VIDEO)

    override fun handlePlaybackInfos(playbackInfos: List<PlaybackInfo>) {
        playbackInfos.forEach { playbackInfo ->
            when (playbackInfo) {
                is PlaybackInfo.MediaUri -> {
                    if (player.currentMediaItem == null) {
                        val mediaItem = MediaItem.fromUri(playbackInfo.uri)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.seekTo(initial.positionMillis)
                        player.playWhenReady = initial.isPlaying
                    }
                }
                is PlaybackInfo.CaptionsUri -> {}
            }
        }
    }

    override fun handleTrackInfoAction(action: TrackInfo.Action) {
        when (action) {
            is TrackInfo.Action.Clear -> player.clearTrackOverrides(action.rendererIndex)
            is TrackInfo.Action.Set -> action.trackInfos.forEach(player::setTrackInfo)
        }
    }

    override fun play() {
        // fixme: reconcile playing when content position is at end
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekRelative(duration: Duration) {
        val current = player.contentPosition.toDuration(DurationUnit.MILLISECONDS)
        val seekTo = current + duration
        seekTo(seekTo)
    }

    override fun seekTo(duration: Duration) {
        player.seekTo(duration.inWholeMilliseconds)
    }

    override fun release() {
        player.release()
    }

    class Factory(
        private val appContext: Context,
        private val trackNameProvider: TrackNameProvider
    ) : AppPlayer.Factory {
        override fun create(initial: PlayerState): AppPlayer {
            val player = SimpleExoPlayer.Builder(appContext).build()
            return ExoPlayerWrapper(player, trackNameProvider, initial)
        }
    }
}