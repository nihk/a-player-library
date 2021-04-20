package library.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import library.common.AppPlayer
import library.common.PlayerEvent
import library.common.PlayerEventStream
import library.common.PlayerState
import library.common.PlayerTelemetry
import library.common.PlayerViewWrapper
import library.common.TrackInfo

class FakeAppPlayerFactory(val appPlayer: AppPlayer) : AppPlayer.Factory {
    var createCount = 0
    override fun create(uri: String): AppPlayer {
        ++createCount
        return appPlayer
    }
}

class FakeAppPlayer : AppPlayer {
    var boundState: PlayerState? = null
    var boundPlayerViewWrapper: PlayerViewWrapper? = null
    var didRelease: Boolean = false
    val collectedEvents = mutableListOf<PlayerEvent>()

    override val state: PlayerState get() = PlayerState.INITIAL
    override val textTracks: List<TrackInfo>
        get() = error("unused")
    override val audioTracks: List<TrackInfo>
        get() = error("unused")
    override val videoTracks: List<TrackInfo>
        get() = error("unused")

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
