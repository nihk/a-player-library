package nick.sample.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import nick.sample.R
import player.common.requireNotNull
import player.ui.common.OnUserLeaveHintViewModel
import player.ui.common.PlayerArguments
import player.ui.common.isMinOsForPip
import player.ui.common.toBundle
import player.ui.common.toPlayerArguments

abstract class PlayerActivity : AppCompatActivity(R.layout.player_activity) {
    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by viewModels()
    private val playerArguments get() = intent.extras?.toPlayerArguments().requireNotNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.container, PlayerFragment.create(playerArguments))
            }
        }
    }

    override fun onUserLeaveHint() {
        onUserLeaveHintViewModel.onUserLeaveHint()
    }

    companion object {
        fun start(context: Context, playerArguments: PlayerArguments) {
            val clazz = if (
                playerArguments.pipConfig?.enabled == true
                && context.isPipAllowed()
            ) {
                PipPlayerActivity::class.java
            } else {
                DefaultPlayerActivity::class.java
            }
            val intent = Intent(context, clazz)
                .putExtras(playerArguments.toBundle())
            context.startActivity(intent)
        }

        private fun Context.isPipAllowed(): Boolean {
            return isMinOsForPip && run {
                val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
        }
    }
}

// If a user doesn't want PiP, there should not be any custom Activity flags set in the AndroidManifest.
internal class DefaultPlayerActivity : PlayerActivity()
// Needed since Activity flags cannot be set programmatically and must be set in AndroidManifest.
internal class PipPlayerActivity : PlayerActivity()