package library.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerState(
    val positionMs: Long,
    val isPlaying: Boolean,
    val trackInfos: List<TrackInfo>
) : Parcelable {

    companion object {
        val INITIAL = PlayerState(
            positionMs = 0L,
            isPlaying = true,
            trackInfos = emptyList()
        )
    }
}