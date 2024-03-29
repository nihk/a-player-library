package player.mediaplayer

import player.common.AppPlayer
import player.common.PlayerEventStream
import player.common.PlayerModule
import player.common.PlayerViewWrapper
import player.common.SeekDataUpdater

class MediaPlayerModule : PlayerModule {
    override val playerViewWrapperFactory: PlayerViewWrapper.Factory get() = MediaPlayerViewWrapper.Factory()
    override val playerEventStream: PlayerEventStream get() = MediaPlayerEventStream()
    override val appPlayerFactory: AppPlayer.Factory get() = MediaPlayerWrapper.Factory()
    override val seekDataUpdater: SeekDataUpdater get() = MediaPlayerSeekDataUpdater()
}
