package player.core

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import player.common.OnUserLeaveHintViewModel
import player.common.PlayerArguments
import player.common.toBundle
import player.common.isMinOsForPip
import player.common.toPlayerArguments

abstract class LibraryActivity : AppCompatActivity(R.layout.library_activity) {

    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    LibraryFragment.create(intent.extras?.toPlayerArguments()!!)
                )
                .commit()
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
                PipLibraryActivity::class.java
            } else {
                DefaultLibraryActivity::class.java
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
internal class DefaultLibraryActivity : LibraryActivity()
// Needed since Activity flags cannot be set programmatically and must be set in AndroidManifest.
internal class PipLibraryActivity : LibraryActivity()