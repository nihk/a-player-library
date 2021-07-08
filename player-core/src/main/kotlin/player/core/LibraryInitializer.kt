package player.core

import player.common.CloseDelegate
import player.common.DefaultPlaybackInfoResolver
import player.common.DefaultTimeFormatter
import player.common.ImageLoader
import player.common.PlaybackInfoResolver
import player.common.PlayerModule
import player.common.PlayerTelemetry
import player.common.ShareDelegate
import player.common.TimeFormatter
import player.ui.common.PlaybackUi
import java.util.*

// Required and optional dependencies to inject into the library.
object LibraryInitializer {
    private var initialized: Boolean = false
    private var playerModule: PlayerModule? = null
    private var telemetry: PlayerTelemetry? = null
    private var shareDelegate: ShareDelegate? = null
    private var closeDelegate: CloseDelegate? = null
    private var playbackInfoResolver: PlaybackInfoResolver? = null
    private var timeFormatter: TimeFormatter? = null
    private var playbackUiFactories: List<PlaybackUi.Factory>? = null
    private var imageLoader: ImageLoader? = null

    internal fun playerModule(): PlayerModule = playerModule.requireInitialized()
    internal fun telemetry(): PlayerTelemetry? = telemetry
    internal fun shareDelegate(): ShareDelegate? = shareDelegate
    internal fun closeDelegate(): CloseDelegate = closeDelegate.requireInitialized()
    internal fun playbackInfoResolver(): PlaybackInfoResolver = playbackInfoResolver.requireInitialized()
    internal fun timeFormatter(): TimeFormatter = timeFormatter.requireInitialized()
    internal fun playbackUiFactories(): List<PlaybackUi.Factory> = playbackUiFactories.requireInitialized()
    internal fun imageLoader(): ImageLoader? = imageLoader

    // Must be called from Application.onCreate().
    fun initialize(
        playerModule: PlayerModule,
        playbackUiFactories: List<PlaybackUi.Factory>,
        telemetry: PlayerTelemetry? = null,
        shareDelegate: ShareDelegate? = null,
        closeDelegate: CloseDelegate? = null,
        playbackInfoResolver: PlaybackInfoResolver? = DefaultPlaybackInfoResolver(),
        timeFormatter: TimeFormatter = DefaultTimeFormatter(Locale.getDefault()),
        imageLoader: ImageLoader? = null
    ) {
        check(!initialized) { "initialize() must only be called once "}
        initialized = true
        LibraryInitializer.playerModule = playerModule
        LibraryInitializer.telemetry = telemetry
        LibraryInitializer.shareDelegate = shareDelegate
        LibraryInitializer.closeDelegate = closeDelegate ?: CloseDelegate()
        LibraryInitializer.playbackInfoResolver = playbackInfoResolver
        LibraryInitializer.timeFormatter = timeFormatter
        LibraryInitializer.playbackUiFactories = playbackUiFactories
        LibraryInitializer.imageLoader = imageLoader
    }

    private fun <T> T?.requireInitialized(): T {
        check(initialized) {
            "Library was not initialized. Call LibraryInitializer.initialize() from Application.onCreate()"
        }

        return requireNotNull(this)
    }
}