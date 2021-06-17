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
import android.util.Rational
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import player.common.PlayerEvent
import player.ui.common.PipController
import player.ui.common.PlayerController
import player.ui.core.R

class NoOpPipController : PipController {
    override fun events(): Flow<PipController.Event> = emptyFlow()
    override fun enterPip() = PipController.Result.DidNotEnterPip
    override fun onEvent(playerEvent: PlayerEvent) = Unit
    override fun isInPip(): Boolean = false

    class Factory : PipController.Factory {
        override fun create(playerController: PlayerController): PipController {
            return NoOpPipController()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class AndroidPipController(
    private val activity: Activity,
    private val playerController: PlayerController
) : PipController {
    // fixme: eliminate this stateful field?
    private var canShowActions = false

    override fun events(): Flow<PipController.Event> = callbackFlow {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action != ActionPip) return

                when (intent.getIntExtra(KeyControl, -1)) {
                    ControlPause -> {
                        trySend(PipController.Event.Pause)
                        updateActions(isPlaying = false)
                    }
                    ControlPlay -> {
                        trySend(PipController.Event.Play)
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

    override fun enterPip(): PipController.Result {
        return try {
            val pipParams = pipParams(playerController.isPlaying())
            activity.enterPictureInPictureMode(pipParams)
            PipController.Result.EnteredPip
        } catch (throwable: Throwable) {
            PipController.Result.DidNotEnterPip
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

        val rational = playerController.aspectRatio()?.let { pair -> Rational(pair.first, pair.second) }

        return PictureInPictureParams.Builder()
            .setActions(actions)
            .setAspectRatio(rational)
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

    class Factory(private val activity: Activity) : PipController.Factory {
        override fun create(playerController: PlayerController): PipController {
            return AndroidPipController(activity, playerController)
        }
    }
}
