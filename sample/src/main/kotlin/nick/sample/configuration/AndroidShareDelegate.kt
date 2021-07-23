package nick.sample.configuration

import android.content.Intent
import androidx.activity.ComponentActivity
import player.ui.common.ShareDelegate

class AndroidShareDelegate : ShareDelegate {
    override fun share(activity: ComponentActivity, uri: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, uri)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        activity.startActivity(shareIntent)
    }
}