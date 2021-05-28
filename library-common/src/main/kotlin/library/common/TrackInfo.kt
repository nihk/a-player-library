package library.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrackInfo(
    val name: String,
    val type: Type,
    val index: Int,
    val groupIndex: Int,
    val rendererIndex: Int,
    val isDefault: Boolean,
    val isSelected: Boolean,
    val isAutoSelected: Boolean,
    val isManuallySet: Boolean
) : Parcelable {

    sealed class Action {
        data class Clear(val rendererIndex: Int) : Action()
        data class Set(val trackInfos: List<TrackInfo>) : Action()
    }

    enum class Type {
        VIDEO,
        AUDIO,
        TEXT
    }
}