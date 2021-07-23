package player.ui.def

import android.content.Context
import player.ui.common.PlayerController
import player.ui.trackspicker.TracksPickerDialog

interface Navigator {
    fun toTracksPicker(
        trackConfig: TrackConfig,
        onDismissed: () -> Unit
    )

    companion object {
        operator fun invoke(
            context: Context,
            playerController: PlayerController
        ): Navigator = Default(context, playerController)
    }

    class Default(
        private val context: Context,
        private val playerController: PlayerController
    ) : Navigator {
        override fun toTracksPicker(
            trackConfig: TrackConfig,
            onDismissed: () -> Unit
        ) {
            TracksPickerDialog(context, playerController, trackConfig.type, trackConfig.filter)
                .apply {
                    setOnDismissListener { onDismissed() }
                }
                .show()
        }
    }
}
