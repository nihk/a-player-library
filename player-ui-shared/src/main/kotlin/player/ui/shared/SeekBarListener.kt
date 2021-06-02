package player.ui.shared

import android.widget.SeekBar
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface SeekBarListener : SeekBar.OnSeekBarChangeListener {
    val isSeekBarBeingTouched: Boolean

    interface Factory {
        fun create(
            updateProgress: (Duration) -> Unit,
            seekTo: (Duration) -> Unit
        ): SeekBarListener
    }
}

class DefaultSeekBarListener(
    private val updateProgress: (Duration) -> Unit,
    private val seekTo: (Duration) -> Unit
) : SeekBarListener {
    private var seekToPosition = Duration.ZERO
    override var isSeekBarBeingTouched = false
        private set

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return

        val position = progress.toDuration(DurationUnit.SECONDS)
        if (isSeekBarBeingTouched) {
            seekToPosition = position
            updateProgress(position)
        } else {
            seekTo(seekToPosition)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        isSeekBarBeingTouched = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        isSeekBarBeingTouched = false
        seekTo(seekToPosition)
    }

    class Factory : SeekBarListener.Factory {
        override fun create(
            updateProgress: (Duration) -> Unit,
            seekTo: (Duration) -> Unit
        ): SeekBarListener {
            return DefaultSeekBarListener(updateProgress, seekTo)
        }
    }
}
