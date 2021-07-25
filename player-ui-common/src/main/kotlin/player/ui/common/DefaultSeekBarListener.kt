package player.ui.common

import android.widget.SeekBar
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class DefaultSeekBarListener(
    private val updateProgress: (Duration) -> Unit,
    private val seekTo: (Duration) -> Unit,
    private val onTrackingTouchChanged: (Boolean) -> Unit
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
        onTrackingTouchChanged(true)
        isSeekBarBeingTouched = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        onTrackingTouchChanged(false)
        isSeekBarBeingTouched = false
        seekTo(seekToPosition)
    }

    class Factory : SeekBarListener.Factory {
        override fun create(
            updateProgress: (Duration) -> Unit,
            seekTo: (Duration) -> Unit,
            onTrackingTouchChanged: (Boolean) -> Unit
        ): SeekBarListener {
            return DefaultSeekBarListener(updateProgress, seekTo, onTrackingTouchChanged)
        }
    }
}