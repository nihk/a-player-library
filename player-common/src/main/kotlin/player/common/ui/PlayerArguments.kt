package player.common.ui

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import player.common.requireNotNull
import java.util.concurrent.TimeUnit

@Parcelize
data class PlayerArguments(
    val uri: String,
    val pipConfig: PictureInPictureConfig? = null,
    val seekConfiguration: SeekConfiguration = SeekConfiguration.DEFAULT,
    val playbackUiFactory: Class<out PlaybackUi.Factory>
) : Parcelable {
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
}

fun PlayerArguments.toBundle(): Bundle {
    return bundleOf("player_arguments" to this)
}

fun Bundle.toPlayerArguments(): PlayerArguments {
    return getParcelable<PlayerArguments>("player_arguments").requireNotNull()
}
