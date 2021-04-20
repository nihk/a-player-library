package library.core

import library.common.PlayerModule
import library.common.PlayerTelemetry
import library.common.ShareDelegate

// Required and optional dependencies to inject into the library.
object LibraryInitializer {
    private var initialized: Boolean = false
    private var playerModule: PlayerModule? = null
    private var telemetry: PlayerTelemetry? = null
    private var shareDelegate: ShareDelegate? = null

    fun playerModule(): PlayerModule = playerModule.requireInitialized()
    fun telemetry(): PlayerTelemetry? = telemetry
    fun shareDelegate(): ShareDelegate? = shareDelegate

    // Must be called from Application.onCreate().
    fun initialize(
        playerModule: PlayerModule,
        telemetry: PlayerTelemetry? = null,
        shareDelegate: ShareDelegate? = null
    ) {
        check(!initialized) { "initialize() must only be called once "}
        initialized = true
        LibraryInitializer.playerModule = playerModule
        LibraryInitializer.telemetry = telemetry
        LibraryInitializer.shareDelegate = shareDelegate
    }

    private fun <T> T?.requireInitialized(): T {
        check(initialized) {
            "Library was not initialized. Call LibraryInitializer.initialize() from Application.onCreate()"
        }

        return requireNotNull(this)
    }
}