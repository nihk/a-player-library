package player.ui.common

import player.common.CloseDelegate
import player.common.ShareDelegate
import player.common.TimeFormatter

data class SharedDependencies(
    val shareDelegate: ShareDelegate?,
    val closeDelegate: CloseDelegate,
    val seekBarListenerFactory: SeekBarListener.Factory,
    val timeFormatter: TimeFormatter,
    val navigator: Navigator
)
