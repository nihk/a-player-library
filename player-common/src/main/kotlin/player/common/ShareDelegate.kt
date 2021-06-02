package player.common

import android.content.Context

interface ShareDelegate {
    fun share(context: Context, uri: String)
}