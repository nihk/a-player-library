package library

import library.common.PlayerModule
import library.common.PlayerTelemetry
import library.common.ShareDelegate
import library.common.PictureInPictureConfig

// Required and optional dependencies to inject into the library.
object LibraryInitializer {
    private var initialized: Boolean = false
    private var playerModule: PlayerModule? = null
    private var telemetry: PlayerTelemetry? = null
    private var shareDelegate: ShareDelegate? = null
    private var pictureInPictureConfig: PictureInPictureConfig? = null

    fun playerModule(): PlayerModule = playerModule.requireInitialized()
    fun telemetry(): PlayerTelemetry? = telemetry
    fun shareDelegate(): ShareDelegate? = shareDelegate
    fun pictureInPictureConfig(): PictureInPictureConfig? = pictureInPictureConfig

    // Must be called from Application.onCreate().
    fun initialize(
        playerModule: PlayerModule,
        telemetry: PlayerTelemetry? = null,
        shareDelegate: ShareDelegate? = null,
        pictureInPictureConfig: PictureInPictureConfig? = null
    ) {
        check(!initialized) { "initialize should only be called once" }
        initialized = true
        this.playerModule = playerModule
        this.telemetry = telemetry
        this.shareDelegate = shareDelegate
        this.pictureInPictureConfig = pictureInPictureConfig
    }

    private fun <T> T?.requireInitialized(): T {
        check(initialized) {
            "Library was not initialized. Call LibraryInitializer.initialize() from Application.onCreate()"
        }

        return requireNotNull(this)
    }
}