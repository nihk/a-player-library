package player.core

import player.common.DefaultPlaybackInfoResolver
import player.common.PlaybackInfoResolver
import player.common.PlayerModule
import player.common.PlayerEventDelegate
import player.ui.common.PlaybackUi

// Required and optional dependencies to inject into the library.
object LibraryInitializer : LibraryConfiguration.Provider {
    private var initialized: Boolean = false
    private var playerModule: PlayerModule? = null
    private var playerEventDelegate: PlayerEventDelegate? = null
    private var playbackInfoResolver: PlaybackInfoResolver? = null
    private var playbackUiFactories: List<PlaybackUi.Factory>? = null

    // Must be called from Application.onCreate().
    fun initialize(
        playerModule: PlayerModule,
        playbackUiFactories: List<PlaybackUi.Factory>,
        playerEventDelegate: PlayerEventDelegate? = null,
        playbackInfoResolver: PlaybackInfoResolver? = DefaultPlaybackInfoResolver(),
    ) {
        check(!initialized) { "initialize() must only be called once "}
        initialized = true
        LibraryInitializer.playerModule = playerModule
        LibraryInitializer.playerEventDelegate = playerEventDelegate
        LibraryInitializer.playbackInfoResolver = playbackInfoResolver
        LibraryInitializer.playbackUiFactories = playbackUiFactories
    }

    override val libraryConfiguration: LibraryConfiguration get() {
        return LibraryConfiguration(
            playerModule = playerModule.requireInitialized(),
            playbackUiFactories = playbackUiFactories.requireInitialized(),
            playerEventDelegate = playerEventDelegate,
            playbackInfoResolver = playbackInfoResolver.requireInitialized(),
        )
    }

    private fun <T> T?.requireInitialized(): T {
        check(initialized) {
            "Library was not initialized. Call LibraryInitializer.initialize() from Application.onCreate()"
        }

        return requireNotNull(this)
    }
}