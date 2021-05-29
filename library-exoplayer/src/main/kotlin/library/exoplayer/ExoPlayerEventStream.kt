package library.exoplayer

import android.util.Log
import android.view.Surface
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import library.common.TAG
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import library.common.AppPlayer
import library.common.PlayerEvent
import library.common.PlayerEventStream
import library.common.PlayerException

internal class ExoPlayerEventStream : PlayerEventStream {
    private val oneTimeEvents = mutableSetOf<Class<out PlayerEvent>>()

    override fun listen(appPlayer: AppPlayer): Flow<PlayerEvent> = callbackFlow {
        appPlayer as? ExoPlayerWrapper ?: error("$appPlayer was not a ${ExoPlayerWrapper::class.java}")

        offer(PlayerEvent.Initial(appPlayer.state))

        val listener = object : Player.EventListener, AnalyticsListener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                offer(PlayerEvent.OnIsLoadingChanged(isLoading))
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                offer(PlayerEvent.OnPlayWhenReadyChanged(playWhenReady, reason))
            }

            override fun onPlaybackStateChanged(state: Int) {
                offer(PlayerEvent.OnPlaybackStateChanged(state))
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) {
                if (PlayerEvent.OnTracksAvailable::class.java !in oneTimeEvents) {
                    oneTimeEvents += PlayerEvent.OnTracksAvailable::class.java
                    offer(PlayerEvent.OnTracksAvailable)
                }
                offer(PlayerEvent.OnTracksChanged)
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                val exception = PlayerException(
                    message = error.message,
                    cause = error.cause
                )
                offer(PlayerEvent.OnPlayerError(exception))
            }

            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                surface: Surface?
            ) {
                offer(PlayerEvent.OnPlayerPrepared)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                offer(PlayerEvent.OnIsPlayingChanged(isPlaying))
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