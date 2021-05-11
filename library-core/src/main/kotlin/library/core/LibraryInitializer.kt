package library.core

import library.common.PlaybackInfoResolver
import library.common.PlayerModule
import library.common.PlayerTelemetry
import library.common.ShareDelegate

// Required and optional dependencies to inject into the library.
object LibraryInitializer {
    private var initialized: Boolean = false
    private var playerModule: PlayerModule? = null
    private var telemetry: PlayerTelemetry? = null
    private var shareDelegate: ShareDelegate? = null
    private var playbackInfoResolver: PlaybackInfoResolver? = null

    fun playerModule(): PlayerModule = playerModule.requireInitialized()
    fun telemetry(): PlayerTelemetry? = telemetry
    fun shareDelegate(): ShareDelegate? = shareDelegate
    fun playbackInfoResolver(): PlaybackInfoResolver? = playbackInfoResolver

    // Must be called from Application.onCreate().
    fun initialize(
        playerModule: PlayerModule,
        telemetry: PlayerTelemetry? = null,
        shareDelegate: ShareDelegate? = null,
        playbackInfoResolver: PlaybackInfoResolver? = null
    ) {
        check(!initialized) { "initialize() must only be called once "}
        initialized = true
        LibraryInitializer.playerModule = playerModule
        LibraryInitializer.telemetry = telemetry
        LibraryInitializer.shareDelegate = shareDelegate
        LibraryInitializer.playbackInfoResolver = playbackInfoResolver
    }

    private fun <T> T?.requireInitialized(): T {
        check(initialized) {
            "Library was not initialized. Call LibraryInitializer.initialize() from Application.onCreate()"
        }

        return requireNotNull(this)
    }
}