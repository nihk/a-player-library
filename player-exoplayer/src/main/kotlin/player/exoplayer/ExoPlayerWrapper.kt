package player.exoplayer

import android.content.Context
import androidx.core.net.toUri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.TrackNameProvider
import player.common.AppPlayer
import player.common.AspectRatio
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
            itemIndex = player.currentWindowIndex,
            positionMillis = player.currentPosition,
            isPlaying = player.isPlaying
        )

    override val tracks: List<TrackInfo>
        get() = player.getTrackInfos(KNOWN_TRACK_TYPES, trackNameProvider)

    override val aspectRatio: AspectRatio
        get() = player.videoSize.let { videoSize ->
            AspectRatio(
                videoSize.width,
                videoSize.height
            )
        }

    override fun handlePlaybackInfos(playbackInfos: List<PlaybackInfo>) {
        val isInitializing = player.currentMediaItem == null

        val captions = playbackInfos.filterIsInstance<PlaybackInfo.Captions>()
            .associate { captions -> captions.mediaUriRef to captions.metadata.toSubtitles() }
        val uris = playbackInfos.filterIsInstance<PlaybackInfo.MediaUri>()
            .map(PlaybackInfo.MediaUri::uri) + playbackInfos.filterIsInstance<PlaybackInfo.RelatedMedia>()
            .map(PlaybackInfo.RelatedMedia::uri)

        val currentMediaItems = player.mediaItems()
        val mediaItems = uris.map { it.uriToMediaItem() }.applySubtitles(captions)

        if (currentMediaItems == mediaItems) {
            // Nothing new to update/add.
            return
        }

        val currentPosition = player.contentPosition
        val currentWindow = player.currentWindowIndex

        player.clearMediaItems()
        player.addMediaItems(mediaItems)

        if (isInitializing) {
            player.prepare()
            player.seekTo(initial.itemIndex, initial.positionMillis)
            player.playWhenReady = initial.isPlaying
        } else {
            player.seekTo(currentWindow, currentPosition)
        }
    }

    private fun ExoPlayer.mediaItems(): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        for (i in 0 until mediaItemCount) {
            mediaItems += getMediaItemAt(i)
        }
        return mediaItems
    }

    private fun List<MediaItem>.applySubtitles(map: Map<String, List<MediaItem.Subtitle>>): List<MediaItem> {
        return map { mediaItem ->
            val subtitles = map[mediaItem.playbackProperties?.uri.toString()]
            if (subtitles != null) {
                mediaItem.buildUpon()
                    .setSubtitles(subtitles)
                    .build()
            } else {
                mediaItem
            }
        }
    }

    private fun String.uriToMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(this)
            .build()
    }

    private fun List<PlaybackInfo.Captions.Metadata>.toSubtitles(): List<MediaItem.Subtitle> {
        return map { metadata ->
            MediaItem.Subtitle(
                metadata.uri.toUri(),
                metadata.mimeType,
                metadata.language
            )
        }
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

    override fun seekRelative(duration: Duration) {
        val current = player.contentPosition.toDuration(DurationUnit.MILLISECONDS)
        val seekTo = current + duration
        seekTo(seekTo)
    }

    override fun seekTo(duration: Duration) {
        player.seekTo(duration.inWholeMilliseconds)
    }

    override fun toPlaylistItem(index: Int) {
        if (player.currentWindowIndex == index) return
        player.seekToDefaultPosition(index)
        player.playWhenReady = true
    }

    override fun hasMedia(): Boolean {
        return player.mediaItemCount != 0
    }

    override fun release() {
        player.release()
    }

    class Factory(
        private val appContext: Context,
        private val trackNameProvider: TrackNameProvider
    ) : AppPlayer.Factory {
        override fun create(initial: PlayerState): AppPlayer {
            val player = SimpleExoPlayer.Builder(appContext)
                .setPauseAtEndOfMediaItems(true)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                }
            return ExoPlayerWrapper(player, trackNameProvider, initial)
        }
    }
}