package library.mediaplayer

import library.common.AppPlayer
import library.common.PlayerEventStream
import library.common.PlayerModule
import library.common.PlayerViewWrapper

class MediaPlayerModule : PlayerModule {
    override val playerViewWrapperFactory: PlayerViewWrapper.Factory get() = MediaPlayerViewWrapper.Factory()
    override val playerEventStream: PlayerEventStream get() = MediaPlayerEventStream()
    override val appPlayerFactory: AppPlayer.Factory get() = MediaPlayerWrapper.Factory()
}