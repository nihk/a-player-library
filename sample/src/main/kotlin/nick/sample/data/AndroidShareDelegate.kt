package nick.sample.data

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import player.ui.common.ShareDelegate

class AndroidShareDelegate : ShareDelegate {
    override fun share(activity: FragmentActivity, uri: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, uri)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        activity.startActivity(shareIntent)
    }
}