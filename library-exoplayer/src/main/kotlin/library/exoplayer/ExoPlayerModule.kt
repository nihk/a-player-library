package library.exoplayer

import android.content.Context
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider
import com.google.android.exoplayer2.ui.TrackNameProvider
import library.common.AppPlayer
import library.common.PlayerEventStream
import library.common.PlayerModule
import library.common.PlayerViewWrapper

class ExoPlayerModule(context: Context) : PlayerModule {

    private val appContext = context.applicationContext
    private val trackNameProvider: TrackNameProvider get() = DefaultTrackNameProvider(appContext.resources)

    override val playerViewWrapperFactory: PlayerViewWrapper.Factory get() = ExoPlayerViewWrapper.Factory()
    override val playerEventStream: PlayerEventStream get() = ExoPlayerEventStream()
    override val appPlayerFactory: AppPlayer.Factory get() = ExoPlayerWrapper.Factory(appContext, trackNameProvider)
}