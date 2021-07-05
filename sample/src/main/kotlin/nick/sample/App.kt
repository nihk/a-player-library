package nick.sample

import android.app.Application
import nick.sample.data.AndroidShareDelegate
import nick.sample.data.LoggingPlayerTelemetry
import nick.sample.data.SampleCloseDelegate
import nick.sample.data.SlowPlaybackInfoResolver
import player.core.LibraryInitializer
import player.exoplayer.ExoPlayerModule
import player.ui.def.DefaultPlaybackUi
import player.ui.sve.SvePlaybackUi

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LibraryInitializer.initialize(
            playerModule = ExoPlayerModule(this),
            // Uncomment to use MediaPlayer APIs instead of ExoPlayer
//            playerModule = MediaPlayerModule(),
            playbackUiFactories = listOf(DefaultPlaybackUi.Factory(), SvePlaybackUi.Factory()),
            telemetry = LoggingPlayerTelemetry(),
            shareDelegate = AndroidShareDelegate(),
            closeDelegate = SampleCloseDelegate(),
            playbackInfoResolver = SlowPlaybackInfoResolver()
        )
    }
}
