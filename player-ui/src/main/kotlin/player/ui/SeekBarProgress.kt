package player.ui

import android.widget.SeekBar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface SeekBarProgress {
    val isSeekBarBeingTouched: Boolean
    fun progress(): Flow<Event>

    sealed class Event {
        data class Progress(val position: Duration) : Event()
        data class SeekTo(val position: Duration) : Event()
    }

    interface Factory {
        fun create(seekBar: SeekBar): SeekBarProgress
    }
}

class DefaultSeekBarProgress(private val seekBar: SeekBar) : SeekBarProgress {
    private var seekToPosition = Duration.ZERO
    override var isSeekBarBeingTouched = false
        private set

    override fun progress(): Flow<SeekBarProgress.Event> = callbackFlow {
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return

                val position = progress.toDuration(DurationUnit.SECONDS)
                if (isSeekBarBeingTouched) {
                    seekToPosition = position
                    trySend(SeekBarProgress.Event.Progress(position))
                } else {
                    trySend(SeekBarProgress.Event.SeekTo(seekToPosition))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSeekBarBeingTouched = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isSeekBarBeingTouched = false
                trySend(SeekBarProgress.Event.SeekTo(seekToPosition))
            }
        }

        seekBar.setOnSeekBarChangeListener(listener)

        awaitClose { seekBar.setOnSeekBarChangeListener(null) }
    }

    class Factory : SeekBarProgress.Factory {
        override fun create(seekBar: SeekBar): SeekBarProgress {
            return DefaultSeekBarProgress(seekBar)
        }
    }
}
