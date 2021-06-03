package player.ui.sve

import player.common.ui.PlaybackUi
import kotlin.time.Duration

data class SveItem(
    val uri: String,
    val imageUri: String,
    val duration: Duration,
    val playbackUiFactory: Class<out PlaybackUi.Factory>
)
