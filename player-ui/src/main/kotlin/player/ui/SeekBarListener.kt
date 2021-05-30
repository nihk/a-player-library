package player.ui

import android.widget.SeekBar
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SeekBarListener(private val seekTo: (Duration) -> Unit) : SeekBar.OnSeekBarChangeListener {
    private var seekToPosition = Duration.ZERO
    var isSeekBarBeingTouched = false
        private set

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) {
            return
        }

        val position = progress.toDuration(DurationUnit.SECONDS)
        if (isSeekBarBeingTouched) {
            /**
             * if the user was touching the seek bar to seek, save this position until
             * they release their pointer.
             */
            seekToPosition = position
        } else {
            /**
             * if the user wasn't touching the seek bar, this was likely triggered by a
             * keyboard or accessibility service, so perform the action right away.
             */
            seekTo(position)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        isSeekBarBeingTouched = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        isSeekBarBeingTouched = false
        seekTo(seekToPosition)
    }
}
