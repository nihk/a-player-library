package nick.sample

import android.app.Application
import nick.sample.data.AndroidShareDelegate
import nick.sample.data.CoilImageLoader
import nick.sample.data.InlineCloseDelegate
import nick.sample.data.LoggingPlayerEventDelegate
import nick.sample.data.SampleCloseDelegate
import nick.sample.data.SampleOnFullscreenChangedCallback
import nick.sample.data.SampleOnVideoSizeChangedCallback
import nick.sample.data.SlowPlaybackInfoResolver
import player.core.LibraryInitializer
import player.exoplayer.ExoPlayerModule
import player.ui.common.TimeFormatter
import player.ui.def.DefaultPlaybackUi
import player.ui.inline.InlinePlaybackUi
import player.ui.sve.SvePlaybackUi
import java.util.*

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val shareDelegate = AndroidShareDelegate()
        val closeDelegate = SampleCloseDelegate()
        val timeFormatter = TimeFormatter(Locale.getDefault())

        LibraryInitializer.initialize(
            playerModule = ExoPlayerModule(this),
            // Uncomment to use MediaPlayer APIs instead of ExoPlayer
//            playerModule = MediaPlayerModule(),
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
                    closeDelegate = InlineCloseDelegate(),
                    onVideoSizeChangedCallback = SampleOnVideoSizeChangedCallback(),
                    onFullscreenChangedCallback = SampleOnFullscreenChangedCallback()
                )
            ),
            playerEventDelegate = LoggingPlayerEventDelegate(),
            playbackInfoResolver = SlowPlaybackInfoResolver()
        )
    }
}
