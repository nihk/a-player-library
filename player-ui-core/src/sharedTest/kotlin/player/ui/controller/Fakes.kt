package player.ui.controller

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.PlayerEventStream
import player.common.PlayerState
import player.common.PlayerTelemetry
import player.common.SeekData
import player.common.SeekDataUpdater
import player.common.TimeFormatter
import player.common.TrackInfo
import kotlin.time.Duration

class FakeAppPlayerFactory(val appPlayer: AppPlayer) : AppPlayer.Factory {
    var createdState: PlayerState? = null
    var createCount = 0
    override fun create(initial: PlayerState): AppPlayer {
        ++createCount
        createdState = initial
        return appPlayer
    }
}

class FakeAppPlayer(
    val fakeTracks: MutableList<TrackInfo> = mutableListOf()
) : AppPlayer {
    var releaseCount = 0
    val collectedEvents = mutableListOf<PlayerEvent>()

    override val state: PlayerState get() = PlayerState.INITIAL
    override val tracks: List<TrackInfo> get() = fakeTracks

    override fun onEvent(playerEvent: PlayerEvent) {
        collectedEvents += playerEvent
    }

    override fun play() {
        error("unused")
    }

    override fun pause() {
        error("unused")
    }

    override fun seekRelative(duration: Duration) {
        error("unused")
    }

    override fun seekTo(duration: Duration) {
        error("unused")
    }

    override fun release() {
        ++releaseCount
    }

    override fun handleTrackInfoAction(action: TrackInfo.Action) = Unit
    override fun handlePlaybackInfos(playbackInfos: List<PlaybackInfo>) = Unit
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

class FakeSeekDataUpdater : SeekDataUpdater {
    override fun seekData(appPlayer: AppPlayer): Flow<SeekData> {
        return emptyFlow()
    }
}

class FakeTimeFormatter : TimeFormatter {
    override fun playerTime(duration: Duration): String {
        return "6:00"
    }
}
