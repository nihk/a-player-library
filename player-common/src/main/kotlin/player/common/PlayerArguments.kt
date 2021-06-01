package player.common

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import java.util.concurrent.TimeUnit

@Parcelize
data class PlayerArguments(
    val mainUri: String,
    val playbackUiType: PlaybackUiType = PlaybackUiType.Default,
    val secondaryUris: List<String> = emptyList(),
    val pipConfig: PictureInPictureConfig? = null,
    val seekConfiguration: SeekConfiguration = SeekConfiguration.DEFAULT
) : Parcelable

@Parcelize
data class SeekConfiguration(
    val forwardAmount: Long,
    val backwardAmount: Long
) : Parcelable {
    companion object {
        val DEFAULT = SeekConfiguration(
            forwardAmount = TimeUnit.SECONDS.toMillis(10L),
            backwardAmount = TimeUnit.SECONDS.toMillis(10L)
        )
    }
}

enum class PlaybackUiType {
    Default,
    ShortVideoExperience
}

fun PlayerArguments.toBundle(): Bundle {
    return bundleOf("player_arguments" to this)
}

fun Bundle.toPlayerArguments(): PlayerArguments {
    return getParcelable("player_arguments")!!
}
