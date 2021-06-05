package player.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerState(
    val itemIndex: Int,
    val positionMillis: Long,
    val isPlaying: Boolean
) : Parcelable {

    companion object {
        val INITIAL = PlayerState(
            itemIndex = 0,
            positionMillis = 0L,
            isPlaying = true
        )
    }
}