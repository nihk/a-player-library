package library.common

interface PlayerModule {
    val playerViewWrapperFactory: PlayerViewWrapper.Factory
    val playerEventStream: PlayerEventStream
    val appPlayerFactory: AppPlayer.Factory
}