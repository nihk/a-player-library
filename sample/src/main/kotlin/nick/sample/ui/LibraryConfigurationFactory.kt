package nick.sample.ui

import androidx.activity.ComponentActivity
import nick.sample.configuration.AndroidShareDelegate
import nick.sample.configuration.CoilImageLoader
import nick.sample.configuration.LoggingPlayerEventDelegate
import nick.sample.configuration.SampleCloseDelegate
import nick.sample.configuration.SampleOnFullscreenChangedCallback
import nick.sample.configuration.SampleOnVideoSizeChangedCallback
import nick.sample.configuration.SlowPlaybackInfoResolver
import player.common.PlaybackInfoResolver
import player.core.LibraryConfiguration
import player.exoplayer.ExoPlayerModule
import player.ui.common.CloseDelegate
import player.ui.common.TimeFormatter
import player.ui.def.DefaultPlaybackUi
import player.ui.inline.InlinePlaybackUi
import player.ui.inline.OnFullscreenChangedCallback
import player.ui.sve.SvePlaybackUi
import java.util.*

class LibraryConfigurationFactory {
    fun create(
        activity: ComponentActivity,
        onFullscreenChangedCallback: OnFullscreenChangedCallback = SampleOnFullscreenChangedCallback(),
        closeDelegate: CloseDelegate = SampleCloseDelegate(),
        isFullscreenInitially: Boolean? = null,
        playbackInfoResolver: PlaybackInfoResolver = SlowPlaybackInfoResolver()
    ): LibraryConfiguration {
        val shareDelegate = AndroidShareDelegate()
        val timeFormatter = TimeFormatter(Locale.getDefault())
        return LibraryConfiguration(
            activity = activity,
            playerModule = ExoPlayerModule(activity.applicationContext),
            playbackUiFactories = listOf(
                DefaultPlaybackUi.Factory(
                    closeDelegate = closeDelegate,
                    shareDelegate = shareDelegate,
                    timeFormatter = timeFormatter
                ),
                SvePlaybackUi.Factory(
                    closeDelegate = closeDelegate,
                    shareDelegate = shareDelegate,
                    imageLoader = CoilImageLoader(),
                    timeFormatter = timeFormatter
                ),
                InlinePlaybackUi.Factory(
                    closeDelegate = closeDelegate,
                    onVideoSizeChangedCallback = SampleOnVideoSizeChangedCallback(),
                    onFullscreenChangedCallback = onFullscreenChangedCallback,
                    isFullscreenInitially = isFullscreenInitially
                )
            ),
            playerEventDelegate = LoggingPlayerEventDelegate(),
            playbackInfoResolver = playbackInfoResolver
        )
    }
}
