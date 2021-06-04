package player.ui.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Useful for Activities informing their Fragments of any [android.app.Activity.onUserLeaveHint] events,
 * which is a typical place to enter Picture-in-Picture mode.
 */
class OnUserLeaveHintViewModel : ViewModel() {
    private val onUserLeaveHints = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun onUserLeaveHints(): Flow<Unit> = onUserLeaveHints

    fun onUserLeaveHint() {
        onUserLeaveHints.tryEmit(Unit)
    }
}