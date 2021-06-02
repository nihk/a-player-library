package nick.sample

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import nick.sample.data.AndroidShareDelegate
import nick.sample.data.LoggingPlayerTelemetry
import nick.sample.data.SlowPlaybackInfoResolver
import player.core.LibraryInitializer
import player.exoplayer.ExoPlayerModule
import player.ui.def.DefaultPlaybackUi
import player.ui.sve.SvePlaybackUi

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LibraryInitializer.initialize(
            playerModule = ExoPlayerModule(this),
            playbackUiFactories = listOf(
                DefaultPlaybackUi.Factory(),
                SvePlaybackUi.Factory()
            ),
            // Uncomment to use MediaPlayer APIs instead of ExoPlayer
//            playerModule = MediaPlayerModule(),
            telemetry = LoggingPlayerTelemetry(),
            shareDelegate = AndroidShareDelegate(),
            playbackInfoResolver = SlowPlaybackInfoResolver()
        )
    }
}