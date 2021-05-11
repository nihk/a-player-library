package library.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import library.common.AppPlayer
import library.common.PlaybackInfoResolver
import library.common.PlayerEvent
import library.common.PlayerEventStream
import library.common.PlayerState
import library.common.PlayerTelemetry
import library.common.PlayerViewWrapper
import library.common.PlaybackInfo
import library.common.TrackInfo

class FakeAppPlayerFactory(val appPlayer: AppPlayer) : AppPlayer.Factory {
    var createCount = 0
    override fun create(playbackInfo: PlaybackInfo): AppPlayer {
        ++createCount
        return appPlayer
    }
}

class FakeAppPlayer(
    val fakeTextTracks: MutableList<TrackInfo> = mutableListOf(),
    val fakeAudioTracks: MutableList<TrackInfo> = mutableListOf(),
    val fakeVideoTracks: MutableList<TrackInfo> = mutableListOf()
) : AppPlayer {
    var boundState: PlayerState? = null
    var boundPlayerViewWrapper: PlayerViewWrapper? = null
    var didRelease: Boolean = false
    val collectedEvents = mutableListOf<PlayerEvent>()

    override val state: PlayerState get() = PlayerState.INITIAL
    override val textTracks: List<TrackInfo> get() = fakeTextTracks
    override val audioTracks: List<TrackInfo> get() = fakeAudioTracks
    override val videoTracks: List<TrackInfo> get() = fakeVideoTracks

    override fun bind(playerViewWrapper: PlayerViewWrapper, playerState: PlayerState?) {
        boundPlayerViewWrapper = playerViewWrapper
        boundState = playerState
    }

    override fun onEvent(playerEvent: PlayerEvent) {
        collectedEvents += playerEvent
    }

    override fun play() {
        error("unused")
    }

    override fun pause() {
        error("unused")
    }

    override fun release() {
        didRelease = true
    }

    override fun handleTrackInfoAction(action: TrackInfo.Action) = Unit
}

class FakePlayerEventStream(val flow: Flow<PlayerEvent> = emptyFlow()) : PlayerEventStream {
    override fun listen(appPlayer: AppPlayer) = flow
}

class FakePlayerTelemetry : PlayerTelemetry {
    val collectedEvents = mutableListOf<PlayerEvent>()
    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        collectedEvents.add(playerEvent)
    }
}

class NoOpPlaybackInfoResolver : PlaybackInfoResolver {
    override suspend fun resolve(uri: String): PlaybackInfo {
        return PlaybackInfo(uri)
    }
}
