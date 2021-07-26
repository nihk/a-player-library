package player.ui.test

import android.widget.SeekBar
import player.ui.common.SeekBarListener
import kotlin.time.Duration

class FakeSeekBarListener : SeekBarListener {
    override val isSeekBarBeingTouched: Boolean = false
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = Unit
    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit

    class Factory(
        private val seekBarListener: SeekBarListener
    ) : SeekBarListener.Factory {
        override fun create(
            updateProgress: (Duration) -> Unit,
            seekTo: (Duration) -> Unit,
            onTrackingTouchChanged: (isTracking: Boolean) -> Unit
        ): SeekBarListener {
            return seekBarListener
        }
    }
}
