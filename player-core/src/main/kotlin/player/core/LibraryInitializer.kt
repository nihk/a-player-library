package player.core

import player.common.DefaultPlaybackInfoResolver
import player.common.DefaultTimeFormatter
import player.common.PlaybackInfoResolver
import player.common.PlayerModule
import player.common.PlayerTelemetry
import player.common.ShareDelegate
import player.common.TimeFormatter
import player.ui.shared.PlaybackUi
import java.util.*

// Required and optional dependencies to inject into the library.
object LibraryInitializer {
    private var initialized: Boolean = false
    private var playerModule: PlayerModule? = null
    private var playbackUiFactories: List<PlaybackUi.Factory>? = null
    private var telemetry: PlayerTelemetry? = null
    private var shareDelegate: ShareDelegate? = null
    private var playbackInfoResolver: PlaybackInfoResolver? = null
    private var timeFormatter: TimeFormatter? = null

    internal fun playerModule(): PlayerModule = playerModule.requireInitialized()
    internal fun playbackUiFactories(): List<PlaybackUi.Factory> = playbackUiFactories.requireInitialized()
    internal fun telemetry(): PlayerTelemetry? = telemetry
    internal fun shareDelegate(): ShareDelegate? = shareDelegate
    internal fun playbackInfoResolver(): PlaybackInfoResolver = playbackInfoResolver.requireInitialized()
    internal fun timeFormatter(): TimeFormatter = timeFormatter.requireInitialized()

    // Must be called from Application.onCreate().
    fun initialize(
        playerModule: PlayerModule,
        playbackUiFactories: List<PlaybackUi.Factory>,
        telemetry: PlayerTelemetry? = null,
        shareDelegate: ShareDelegate? = null,
        playbackInfoResolver: PlaybackInfoResolver? = DefaultPlaybackInfoResolver(),
        timeFormatter: TimeFormatter = DefaultTimeFormatter(Locale.getDefault())
    ) {
        check(!initialized) { "initialize() must only be called once "}
        require(playbackUiFactories.isNotEmpty()) { "PlaybackUi.Factories must not be empty" }
        initialized = true
        LibraryInitializer.playerModule = playerModule
        LibraryInitializer.playbackUiFactories = playbackUiFactories
        LibraryInitializer.telemetry = telemetry
        LibraryInitializer.shareDelegate = shareDelegate
        LibraryInitializer.playbackInfoResolver = playbackInfoResolver
        LibraryInitializer.timeFormatter = timeFormatter
    }

    private fun <T> T?.requireInitialized(): T {
        check(initialized) {
            "Library was not initialized. Call LibraryInitializer.initialize() from Application.onCreate()"
        }

        return requireNotNull(this)
    }
}