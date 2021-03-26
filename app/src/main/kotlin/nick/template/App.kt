package nick.template

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import library.LibraryInitializer
import library.common.PictureInPictureConfig
import nick.template.data.AndroidShareDelegate
import nick.template.data.LoggingPlayerTelemetry
import library.exoplayer.ExoPlayerModule

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LibraryInitializer.initialize(
            playerModule = ExoPlayerModule(this),
            // Uncomment to use MediaPlayer APIs instead of ExoPlayer
//            playerModule = MediaPlayerModule(),
            telemetry = LoggingPlayerTelemetry(),
            shareDelegate = AndroidShareDelegate(),
            pictureInPictureConfig = PictureInPictureConfig(
                onBackPresses = false,
                onUserLeaveHints = false
            )
        )
    }
}