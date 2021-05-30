package player.exoplayer

import android.util.Log
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import player.common.AppPlayer
import player.common.PlayerEvent
import player.common.PlayerEventStream
import player.common.PlayerException
import player.common.TAG

internal class ExoPlayerEventStream : PlayerEventStream {
    private val oneTimeEvents = mutableSetOf<Class<out PlayerEvent>>()

    override fun listen(appPlayer: AppPlayer): Flow<PlayerEvent> = callbackFlow {
        appPlayer as? ExoPlayerWrapper ?: error("$appPlayer was not a ${ExoPlayerWrapper::class.java}")

        trySend(PlayerEvent.Initial)

        val listener = object : Player.Listener, AnalyticsListener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                trySend(PlayerEvent.OnIsLoadingChanged(isLoading))
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                trySend(PlayerEvent.OnPlayWhenReadyChanged(playWhenReady, reason))
            }

            override fun onPlaybackStateChanged(state: Int) {
                trySend(PlayerEvent.OnPlaybackStateChanged(state))
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) {
                if (PlayerEvent.OnTracksAvailable::class.java !in oneTimeEvents) {
                    oneTimeEvents += PlayerEvent.OnTracksAvailable::class.java
                    trySend(PlayerEvent.OnTracksAvailable)
                }
                trySend(PlayerEvent.OnTracksChanged)
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                val exception = PlayerException(
                    message = error.message,
                    cause = error.cause
                )
                trySend(PlayerEvent.OnPlayerError(exception))
            }

            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                output: Any,
                renderTimeMs: Long
            ) {
                trySend(PlayerEvent.OnPlayerPrepared)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                trySend(PlayerEvent.OnIsPlayingChanged(isPlaying))
            }
        }

        appPlayer.player.run {
            addListener(listener)
            (this as? SimpleExoPlayer)?.addAnalyticsListener(listener)
        }

        awaitClose {
            Log.d(TAG, "Removing player listeners")
            appPlayer.player.run {
                removeListener(listener)
                (this as? SimpleExoPlayer)?.removeAnalyticsListener(listener)
            }
            oneTimeEvents.clear()
        }
    }
}