package library.mediaplayer

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import library.common.AppPlayer
import library.common.PlayerEvent
import library.common.PlayerEventStream
import library.common.PlayerException
import library.common.TAG

private interface Listener :
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnBufferingUpdateListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnInfoListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnVideoSizeChangedListener

internal class MediaPlayerEventStream : PlayerEventStream {
    override fun listen(appPlayer: AppPlayer): Flow<PlayerEvent> = callbackFlow {
        appPlayer as? MediaPlayerWrapper ?: error("$appPlayer was not a ${MediaPlayerWrapper::class.java}")

        val listener = object : Listener {
            override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
                val exception = PlayerException(message = "what: $what, extra: $extra")
                offer(PlayerEvent.OnPlayerError(exception))
                return false
            }

            override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
                offer(PlayerEvent.OnInfo(what, extra))
                return false
            }

            override fun onPrepared(mp: MediaPlayer) {
                offer(PlayerEvent.OnPlayerPrepared)
                offer(PlayerEvent.OnTracksAvailable)
            }

            override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
                offer(PlayerEvent.OnVideoSizeChanged(width = width, height = height))
            }

            override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {

            }

            override fun onCompletion(mp: MediaPlayer) {

            }
        }

        appPlayer.mediaPlayer.run {
            setOnErrorListener(listener)
            setOnBufferingUpdateListener(listener)
            setOnCompletionListener(listener)
            setOnInfoListener(listener)
            setOnPreparedListener(listener)
            setOnVideoSizeChangedListener(listener)
        }

        awaitClose {
            appPlayer.mediaPlayer.run {
                Log.d(TAG, "Closing player event stream")
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