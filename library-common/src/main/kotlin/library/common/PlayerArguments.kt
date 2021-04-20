package library.common

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerArguments(
    val uri: String,
    val pipConfig: PictureInPictureConfig? = null
) : Parcelable

fun PlayerArguments.bundle(): Bundle {
    return bundleOf("player_arguments" to this)
}

fun Bundle.toPlayerArguments(): PlayerArguments {
    return getParcelable("player_arguments")!!
}