package player.common.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PictureInPictureConfig(
    val enabled: Boolean = true,
    val onBackPresses: Boolean = false
) : Parcelable