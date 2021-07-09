package player.core

import player.common.DefaultPlaybackInfoResolver
import player.common.PlaybackInfoResolver
import player.common.PlayerModule
import player.common.PlayerTelemetry
import player.ui.common.PlaybackUi

// Required and optional dependencies to inject into the library.
object LibraryInitializer {
    private var initialized: Boolean = false
    private var playerModule: PlayerModule? = null
    private var telemetry: PlayerTelemetry? = null
    private var playbackInfoResolver: PlaybackInfoResolver? = null
    private var playbackUiFactories: List<PlaybackUi.Factory>? = null

    internal fun playerModule(): PlayerModule = playerModule.requireInitialized()
    internal fun telemetry(): PlayerTelemetry? = telemetry
    internal fun playbackInfoResolver(): PlaybackInfoResolver = playbackInfoResolver.requireInitialized()
    internal fun playbackUiFactories(): List<PlaybackUi.Factory> = playbackUiFactories.requireInitialized()

    // Must be called from Application.onCreate().
    fun initialize(
        playerModule: PlayerModule,
        playbackUiFactories: List<PlaybackUi.Factory>,
        telemetry: PlayerTelemetry? = null,
        playbackInfoResolver: PlaybackInfoResolver? = DefaultPlaybackInfoResolver(),
    ) {
        check(!initialized) { "initialize() must only be called once "}
        initialized = true
        LibraryInitializer.playerModule = playerModule
        LibraryInitializer.telemetry = telemetry
        LibraryInitializer.playbackInfoResolver = playbackInfoResolver
        LibraryInitializer.playbackUiFactories = playbackUiFactories
    }

    private fun <T> T?.requireInitialized(): T {
        check(initialized) {
            "Library was not initialized. Call LibraryInitializer.initialize() from Application.onCreate()"
        }

        return requireNotNull(this)
    }
}