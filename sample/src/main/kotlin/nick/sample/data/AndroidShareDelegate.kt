package nick.sample.data

import android.content.Context
import android.content.Intent
import player.common.ShareDelegate

class AndroidShareDelegate : ShareDelegate {
    override fun share(context: Context, uri: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, uri)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }
}