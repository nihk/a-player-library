package nick.sample.data

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import nick.sample.R
import player.ui.inline.OnFullscreenChangedCallback

class SampleOnFullscreenChangedCallback : OnFullscreenChangedCallback {
    override fun onFullscreenChanged(isFullscreen: Boolean, activity: FragmentActivity) {
        val newParent: ViewGroup = if (isFullscreen) {
            activity.findViewById(R.id.fullscreen_container)
        } else {
            activity.findViewById(R.id.smallscreen_container)
        }

        val movable = activity.findViewById<View>(R.id.movable_container)
        movable.detachFromParent()
        newParent.addView(movable)
    }
}
