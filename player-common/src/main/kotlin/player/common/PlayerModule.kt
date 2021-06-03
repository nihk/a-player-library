package player.common

import player.common.ui.PlayerViewWrapper

interface PlayerModule {
    val playerViewWrapperFactory: PlayerViewWrapper.Factory
    val playerEventStream: PlayerEventStream
    val appPlayerFactory: AppPlayer.Factory
    val seekDataUpdater: SeekDataUpdater
}