package nick.template

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import library.core.LibraryInitializer
import library.exoplayer.ExoPlayerModule
import nick.template.data.AndroidShareDelegate
import nick.template.data.LoggingPlayerTelemetry

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LibraryInitializer.initialize(
            playerModule = ExoPlayerModule(this),
            // Uncomment to use MediaPlayer APIs instead of ExoPlayer
//            playerModule = MediaPlayerModule(),
            telemetry = LoggingPlayerTelemetry(),
            shareDelegate = AndroidShareDelegate()
        )
    }
}