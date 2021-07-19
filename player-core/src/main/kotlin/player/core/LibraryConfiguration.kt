package player.core

import player.common.DefaultPlaybackInfoResolver
import player.common.PlaybackInfoResolver
import player.common.PlayerEventDelegate
import player.common.PlayerModule
import player.ui.common.PlaybackUi

data class LibraryConfiguration(
    val playerModule: PlayerModule,
    val playbackUiFactories: List<PlaybackUi.Factory>,
    val playerEventDelegate: PlayerEventDelegate? = null,
    val playbackInfoResolver: PlaybackInfoResolver = DefaultPlaybackInfoResolver(),
) {
    interface Provider {
        val libraryConfiguration: LibraryConfiguration
    }
}
