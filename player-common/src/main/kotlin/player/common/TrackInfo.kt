package player.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrackInfo(
    val name: String?,
    val type: Type,
    val size: Size,
    val indices: Indices,
    val isDefault: Boolean,
    val isSelected: Boolean,
    val isAutoSelected: Boolean,
    val isManuallySet: Boolean
) : Parcelable {

    @Parcelize
    data class Size(
        val width: Int,
        val height: Int
    ) : Parcelable

    @Parcelize
    data class Indices(
        val index: Int,
        val groupIndex: Int,
        val rendererIndex: Int
    ) : Parcelable

    enum class Type {
        VIDEO,
        AUDIO,
        TEXT
    }
}