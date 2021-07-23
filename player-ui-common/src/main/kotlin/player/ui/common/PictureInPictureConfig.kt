package player.ui.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PictureInPictureConfig(
    val onBackPresses: Boolean = false,
    val onUserLeaveHints: Boolean = true
) : Parcelable {
    val isEnabled: Boolean get() = onBackPresses || onUserLeaveHints
}