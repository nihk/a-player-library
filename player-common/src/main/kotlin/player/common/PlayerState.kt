package player.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerState(
    val positionMillis: Long,
    val isPlaying: Boolean
) : Parcelable {

    companion object {
        val INITIAL = PlayerState(
            positionMillis = 0L,
            isPlaying = true
        )
    }
}