package player.exoplayer

import android.content.Context
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider
import com.google.android.exoplayer2.ui.TrackNameProvider
import player.common.AppPlayer
import player.common.PlayerEventStream
import player.common.PlayerModule
import player.common.PlayerViewWrapper
import player.common.SeekDataUpdater

class ExoPlayerModule(context: Context) : PlayerModule {

    private val appContext = context.applicationContext
    private val trackNameProvider: TrackNameProvider get() = DefaultTrackNameProvider(appContext.resources)

    override val playerViewWrapperFactory: PlayerViewWrapper.Factory get() = ExoPlayerViewWrapper.Factory()
    override val playerEventStream: PlayerEventStream get() = ExoPlayerEventStream(trackNameProvider)
    override val appPlayerFactory: AppPlayer.Factory get() = ExoPlayerWrapper.Factory(appContext, trackNameProvider)
    override val seekDataUpdater: SeekDataUpdater get() = ExoPlayerSeekDataUpdater()
}
