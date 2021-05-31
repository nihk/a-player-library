package player.exoplayer

import android.content.Context
import androidx.core.net.toUri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
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
        get() = PlayerState(
            positionMillis = player.currentPosition,
            isPlaying = player.isPlaying
        )

    override val tracks: List<TrackInfo>
        get() = player.getTrackInfos(KNOWN_TRACK_TYPES, trackNameProvider)

    override fun handlePlaybackInfos(playbackInfos: List<PlaybackInfo>) {
        val currentMediaItem = player.currentMediaItem
        val captions = playbackInfos.filterIsInstance<PlaybackInfo.Captions>().firstOrNull()

        if (currentMediaItem == null) {
            val mediaUri = playbackInfos.filterIsInstance<PlaybackInfo.MediaUri>().firstOrNull()
            if (mediaUri != null) {
                val mediaItem = MediaItem.Builder()
                    .setUri(mediaUri.uri)
                    .addCaptions(captions)
                    .build()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.seekTo(initial.positionMillis)
                player.playWhenReady = initial.isPlaying
            }
        } else {
            val currentSubtitles = currentMediaItem.playbackProperties?.subtitles ?: emptyList()
            if (captions != null && !currentSubtitles.contains(captions)) {
                // Load subtitles that came in after media content had already started
                val mediaItem = currentMediaItem.buildUpon()
                    .addCaptions(captions)
                    .build()
                val contentPosition = player.contentPosition
                val playWhenReady = player.playWhenReady
                player.setMediaItem(mediaItem)
                player.prepare()
                player.seekTo(contentPosition)
                player.playWhenReady = playWhenReady
            }
        }
    }

    private fun List<MediaItem.Subtitle>.contains(captions: PlaybackInfo.Captions): Boolean {
        return map { it.uri.toString() }.containsAll(captions.metadata.map { it.uri })
    }

    private fun MediaItem.Builder.addCaptions(captions: PlaybackInfo.Captions?): MediaItem.Builder {
        if (captions == null) {
            return this
        }

        val subtitles = captions.metadata.map { metadata ->
            MediaItem.Subtitle(
                metadata.uri.toUri(),
                metadata.mimeType,
                metadata.language
            )
        }
        return setSubtitles(subtitles)
    }

    override fun handleTrackInfoAction(action: TrackInfo.Action) {
        when (action) {
            is TrackInfo.Action.Clear -> player.clearTrackOverrides(action.rendererIndex)
            is TrackInfo.Action.Set -> action.trackInfos.forEach(player::setTrackInfo)
        }
    }

    override fun play() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(player.currentWindowIndex, C.TIME_UNSET)
        }
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