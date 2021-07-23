package player.ui.inline

import androidx.activity.ComponentActivity

interface OnFullscreenChangedCallback {
    fun onFullscreenChanged(isFullscreen: Boolean, activity: ComponentActivity)
}
