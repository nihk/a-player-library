package player.exoplayer

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import player.common.AppPlayer
import player.common.SeekData
import player.common.SeekDataUpdater
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// todo: look into merging trySend/emit APIs
class ExoPlayerSeekDataUpdater : SeekDataUpdater {
    override fun seekData(appPlayer: AppPlayer): Flow<SeekData> {
        appPlayer as? ExoPlayerWrapper ?: error("$appPlayer was not a ${ExoPlayerWrapper::class.java}")
        val player = appPlayer.player
        return merge(eventBased(player), tick(player))
            .conflate()
    }

    private fun eventBased(player: Player): Flow<SeekData> = callbackFlow {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updateSeekData(player)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateSeekData(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateSeekData(player)
            }
        }

        player.addListener(listener)

        awaitClose {
            player.removeListener(listener)
        }
    }

    private fun SendChannel<SeekData>.updateSeekData(player: Player) {
        when (val command = player.toCommand()) {
            is Command.Emit -> trySend(command.seekData)
        }
    }

    private fun tick(player: Player): Flow<SeekData> = flow {
        while (currentCoroutineContext().isActive) {
            when (val command = player.toCommand()) {
                is Command.Emit -> emit(command.seekData)
            }
            delay(200.toDuration(DurationUnit.MILLISECONDS))
        }
    }

    private fun Player.toCommand(): Command {
        val ignore = contentDuration == C.TIME_UNSET
        return if (ignore) {
            Command.DoNotEmit
        } else {
            val seekData = SeekData(
                position = contentPosition.toDuration(DurationUnit.MILLISECONDS),
                buffered = contentBufferedPosition.toDuration(DurationUnit.MILLISECONDS),
                duration = contentDuration.toDuration(DurationUnit.MILLISECONDS)
            )
            Command.Emit(seekData)
        }
    }

    private sealed class Command {
        data class Emit(val seekData: SeekData) : Command()
        object DoNotEmit : Command()
    }
}
