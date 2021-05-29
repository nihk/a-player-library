package library.mediaplayer

import android.media.MediaPlayer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import library.common.AppPlayer
import library.common.SeekData
import library.common.SeekDataUpdater
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MediaPlayerSeekDataUpdater : SeekDataUpdater {
    override fun seekData(appPlayer: AppPlayer) = flow {
        appPlayer as? MediaPlayerWrapper ?: error("$appPlayer was not a ${MediaPlayerWrapper::class.java}")
        val mediaPlayer = appPlayer.mediaPlayer
        mediaPlayer.awaitPrepared()

        while (currentCoroutineContext().isActive) {
            val seekData = SeekData(
                position = mediaPlayer.currentPosition.toDuration(DurationUnit.MILLISECONDS),
                buffered = mediaPlayer.currentPosition.toDuration(DurationUnit.MILLISECONDS),
                duration = mediaPlayer.duration.toDuration(DurationUnit.MILLISECONDS),
            )
            emit(seekData)
            delay(poll)
        }
    }

    private suspend fun MediaPlayer.awaitPrepared() {
        // MediaPlayer only allows 1 listener per setOn*Listener() (which MediaPlayerEventStream
        // uses) so I cannot install one here. Use this as a mediocre workaround.
        while (currentCoroutineContext().isActive && !isPlaying) {
            delay(poll)
        }
    }

    companion object {
        private val poll = 500.toDuration(DurationUnit.MILLISECONDS)
    }
}
