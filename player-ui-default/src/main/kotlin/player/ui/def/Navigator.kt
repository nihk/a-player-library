package player.ui.def

import android.content.Context
import player.common.TrackInfo
import player.ui.common.PlayerController
import player.ui.trackspicker.TracksPickerDialog

interface Navigator {
    fun toTracksPicker(type: TrackInfo.Type)

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
        override fun toTracksPicker(type: TrackInfo.Type) {
            TracksPickerDialog(context, playerController, type)
                .show()
        }
    }
}
