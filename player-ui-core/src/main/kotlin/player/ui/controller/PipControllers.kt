package player.ui.controller

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
import androidx.core.app.ComponentActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import player.common.PlayerEvent
import player.common.VideoSize
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
    private val activity: ComponentActivity,
    private val playerController: PlayerController
) : PipController {

    override fun events(): Flow<PipController.Event> = callbackFlow {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action != ActionPip) return

                when (intent.getIntExtra(KeyControl, -1)) {
                    ControlPause -> {
                        playerController.pause()
                        trySend(PipController.Event.Pause)
                    }
                    ControlPlay -> {
                        playerController.play()
                        trySend(PipController.Event.Play)
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
        if (!activity.isInPictureInPictureMode) return

        when (playerEvent) {
            is PlayerEvent.OnPlayerPrepared -> updateActions(isPlaying = playerEvent.playWhenReady)
            is PlayerEvent.OnIsPlayingChanged -> updateActions(isPlaying = playerEvent.isPlaying)
            is PlayerEvent.OnVideoSizeChanged -> updateActions(isPlaying = playerController.isPlaying())
        }
    }

    override fun isInPip(): Boolean = activity.isInPictureInPictureMode

    private fun pipParams(isPlaying: Boolean): PictureInPictureParams {
        val actions = if (playerController.hasMedia()) {
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
            .setAspectRatio(playerController.videoSize()?.asRational())
            .build()
    }

    private fun VideoSize.asRational(): Rational {
        return if (isUnknown) {
            return DefaultAspectRatio
        } else {
            Rational(widthPx, heightPx)
        }
    }

    private val VideoSize.isUnknown: Boolean get() = widthPx == 0 && heightPx == 0

    private fun updateActions(isPlaying: Boolean) {
        val pipParams = pipParams(isPlaying)
        activity.setPictureInPictureParams(pipParams)
    }

    private fun pauseAction(): RemoteAction {
        return remoteAction(
            requestCode = RequestPause,
            control = ControlPause,
            iconDrawable = R.drawable.pip_pause,
            title = activity.getString(R.string.pause)
        )
    }

    private fun playAction(): RemoteAction {
        return remoteAction(
            requestCode = RequestPlay,
            control = ControlPlay,
            iconDrawable = R.drawable.pip_play,
            title = activity.getString(R.string.play)
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
        private const val KeyControl = "control"
        private const val ActionPip = "action_pip"
        private const val RequestPause = 0
        private const val RequestPlay = 1
        private const val ControlPause = 0
        private const val ControlPlay = 1

        private val DefaultAspectRatio = Rational(16, 9)
    }

    class Factory(private val activity: ComponentActivity) : PipController.Factory {
        override fun create(playerController: PlayerController): PipController {
            return AndroidPipController(activity, playerController)
        }
    }
}
