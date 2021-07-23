package player.ui.common

import androidx.activity.ComponentActivity

interface ShareDelegate {
    fun share(activity: ComponentActivity, uri: String)
}
