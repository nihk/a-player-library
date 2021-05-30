package player.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerState(
    val positionMs: Long,
    val isPlaying: Boolean
) : Parcelable {

    companion object {
        val INITIAL = PlayerState(
            positionMs = 0L,
            isPlaying = true
        )
    }
}