package player.ui.sve

import kotlin.time.Duration

data class SveItem(
    val uri: String,
    val imageUri: String,
    val duration: Duration,
    val playbackUiFactory: Class<*>
)
