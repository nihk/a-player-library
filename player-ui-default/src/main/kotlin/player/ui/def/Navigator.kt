package player.ui.def

import android.content.Context
import player.ui.common.PlayerController
import player.ui.trackspicker.TracksPickerConfig
import player.ui.trackspicker.TracksPickerDialog

interface Navigator {
    fun toTracksPicker(
        config: TracksPickerConfig,
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
            config: TracksPickerConfig,
            onDismissed: () -> Unit
        ) {
            TracksPickerDialog(context, playerController, config)
                .apply {
                    setOnDismissListener { onDismissed() }
                }
                .show()
        }
    }
}
