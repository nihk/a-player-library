package player.mediaplayer

import android.media.MediaPlayer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import player.common.AppPlayer
import player.common.PlayerEvent
import player.common.PlayerEventStream
import player.common.PlayerException

internal class MediaPlayerEventStream : PlayerEventStream {
    override fun listen(appPlayer: AppPlayer): Flow<PlayerEvent> = callbackFlow {
        appPlayer as? MediaPlayerWrapper ?: error("$appPlayer was not a ${MediaPlayerWrapper::class.java}")
        val mediaPlayer = appPlayer.mediaPlayer

        trySend(PlayerEvent.Initial)

        val listener = object :
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnBufferingUpdateListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnInfoListener,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnVideoSizeChangedListener {
            override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
                val exception = PlayerException(message = "what: $what, extra: $extra")
                trySend(PlayerEvent.OnPlayerError(exception))
                return false
            }

            override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
                trySend(PlayerEvent.OnInfo(what, extra))
                return false
            }

            override fun onPrepared(mp: MediaPlayer) {
                trySend(PlayerEvent.OnPlayerPrepared(mp.isPlaying))
            }

            override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
                trySend(PlayerEvent.OnVideoSizeChanged(width = width, height = height))
            }

            override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {

            }

            override fun onCompletion(mp: MediaPlayer) {

            }
        }

        mediaPlayer.run {
            setOnErrorListener(listener)
            setOnBufferingUpdateListener(listener)
            setOnCompletionListener(listener)
            setOnInfoListener(listener)
            setOnPreparedListener(listener)
            setOnVideoSizeChangedListener(listener)
        }

        awaitClose {
            mediaPlayer.run {
                setOnErrorListener(null)
                setOnBufferingUpdateListener(null)
                setOnCompletionListener(null)
                setOnInfoListener(null)
                setOnPreparedListener(null)
                setOnVideoSizeChangedListener(null)
            }
        }
    }
}