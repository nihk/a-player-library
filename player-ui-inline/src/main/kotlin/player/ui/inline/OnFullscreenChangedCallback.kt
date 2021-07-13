package player.ui.inline

import androidx.fragment.app.FragmentActivity

interface OnFullscreenChangedCallback {
    fun onFullscreenChanged(isFullscreen: Boolean, activity: FragmentActivity)
}
