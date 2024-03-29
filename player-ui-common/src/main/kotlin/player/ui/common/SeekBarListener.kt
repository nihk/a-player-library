package player.ui.common

import android.widget.SeekBar
import kotlin.time.Duration

interface SeekBarListener : SeekBar.OnSeekBarChangeListener {
    val isSeekBarBeingTouched: Boolean

    interface Factory {
        fun create(
            updateProgress: (Duration) -> Unit,
            seekTo: (Duration) -> Unit,
            onTrackingTouchChanged: (isTracking: Boolean) -> Unit = {}
        ): SeekBarListener
    }
}
