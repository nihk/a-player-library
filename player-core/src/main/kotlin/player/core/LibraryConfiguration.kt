package player.core

import androidx.activity.ComponentActivity
import player.common.PlaybackInfoResolver
import player.common.PlayerEventDelegate
import player.common.PlayerModule
import player.ui.common.PlaybackUi

data class LibraryConfiguration(
    val activity: ComponentActivity,
    val playerModule: PlayerModule,
    val playbackUiFactories: List<PlaybackUi.Factory>,
    val playerEventDelegate: PlayerEventDelegate? = null,
    val playbackInfoResolver: PlaybackInfoResolver = PlaybackInfoResolver(),
)
