package player.ui.controller

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import player.common.PlayerEvent
import player.ui.R
import player.ui.shared.EnterPipResult
import player.ui.shared.PipController
import player.ui.shared.PipEvent

class NoOpPipController : PipController {
    override fun events(): Flow<PipEvent> = emptyFlow()
    override fun enterPip(isPlaying: Boolean) = EnterPipResult.DidNotEnterPip
    override fun onEvent(playerEvent: PlayerEvent) = Unit
    override fun isInPip(): Boolean = false
}

@RequiresApi(Build.VERSION_CODES.O)
class AndroidPipController(private val activity: Activity) : PipController {
    private var canShowActions = false

    override fun events(): Flow<PipEvent> = callbackFlow {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action != ActionPip) return

                when (intent.getIntExtra(KeyControl, -1)) {
                    ControlPause -> {
                        trySend(PipEvent.Pause)
                        updateActions(isPlaying = false)
                    }
                    ControlPlay -> {
                        trySend(PipEvent.Play)
                        updateActions(isPlaying = true)
                    }
                }
            }
        }

        activity.registerReceiver(
            broadcastReceiver,
            IntentFilter(ActionPip)
        )

        awaitClose { activity.unregisterReceiver(broadcastReceiver) }
    }

    override fun enterPip(isPlaying: Boolean): EnterPipResult {
        return try {
            val pipParams = pipParams(isPlaying)
            activity.enterPictureInPictureMode(pipParams)
            EnterPipResult.EnteredPip
        } catch (throwable: Throwable) {
            EnterPipResult.DidNotEnterPip
        }
    }

    override fun onEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.OnPlayerPrepared -> {
                if (canShowActions || !activity.isInPictureInPictureMode) return
                canShowActions = true
                updateActions(isPlaying = playerEvent.playWhenReady)
            }
        }
    }

    override fun isInPip(): Boolean = activity.isInPictureInPictureMode

    private fun pipParams(isPlaying: Boolean): PictureInPictureParams {
        val actions = if (canShowActions) {
            listOf(
                if (isPlaying) {
                    pauseAction()
                } else {
                    playAction()
                }
            )
        } else {
            emptyList()
        }

        return PictureInPictureParams.Builder()
            .setActions(actions)
            .build()
    }

    private fun updateActions(isPlaying: Boolean) {
        val pipParams = pipParams(isPlaying)
        activity.setPictureInPictureParams(pipParams)
    }

    private fun pauseAction(): RemoteAction {
        return remoteAction(
            requestCode = RequestPause,
            control = ControlPause,
            iconDrawable = R.drawable.pip_pause,
            title = "Pause"
        )
    }

    private fun playAction(): RemoteAction {
        return remoteAction(
            requestCode = RequestPlay,
            control = ControlPlay,
            iconDrawable = R.drawable.pip_play,
            title = "Play"
        )
    }

    private fun remoteAction(
        requestCode: Int,
        control: Int,
        @DrawableRes iconDrawable: Int,
        title: String,
        contentDescription: String = title
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(activity, iconDrawable),
            title,
            contentDescription,
            PendingIntent.getBroadcast(
                activity,
                requestCode,
                Intent(ActionPip).putExtra(KeyControl, control),
                0
            )
        )
    }

    companion object {
        const val KeyControl = "control"
        const val ActionPip = "action_pip"
        const val RequestPause = 0
        const val RequestPlay = 1
        const val ControlPause = 0
        const val ControlPlay = 1
    }
}
