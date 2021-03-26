package nick.template.data

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import library.common.ShareDelegate

class AndroidShareDelegate : ShareDelegate {
    override fun share(activity: FragmentActivity, url: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        activity.startActivity(shareIntent)
    }
}