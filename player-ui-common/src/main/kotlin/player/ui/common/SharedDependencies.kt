package player.ui.common

import android.content.Context
import player.common.ShareDelegate
import player.common.TimeFormatter

data class SharedDependencies(
    val context: Context,
    val shareDelegate: ShareDelegate?,
    val seekBarListenerFactory: SeekBarListener.Factory,
    val timeFormatter: TimeFormatter,
    val navigator: Navigator
)
