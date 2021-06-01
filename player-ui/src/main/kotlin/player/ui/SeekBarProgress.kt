package player.ui

import android.widget.SeekBar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SeekBarProgress(private val seekBar: SeekBar) {
    private var seekToPosition = Duration.ZERO
    var isSeekBarBeingTouched = false
        private set

    fun progress(): Flow<Event> = callbackFlow {
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return

                val position = progress.toDuration(DurationUnit.SECONDS)
                if (isSeekBarBeingTouched) {
                    seekToPosition = position
                    trySend(Event.Progress(position))
                } else {
                    trySend(Event.SeekTo(seekToPosition))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSeekBarBeingTouched = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isSeekBarBeingTouched = false
                trySend(Event.SeekTo(seekToPosition))
            }
        }

        seekBar.setOnSeekBarChangeListener(listener)

        awaitClose { seekBar.setOnSeekBarChangeListener(null) }
    }

    sealed class Event {
        data class Progress(val position: Duration) : Event()
        data class SeekTo(val position: Duration) : Event()
    }
}
