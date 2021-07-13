package nick.sample.data

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import nick.sample.R
import player.ui.common.CloseDelegate

class InlineCloseDelegate : CloseDelegate {
    override fun onClose(activity: FragmentActivity) {
        val fragment =  activity.supportFragmentManager.findFragmentById(R.id.movable_container)
            ?: return

        activity.supportFragmentManager.commit {
            remove(fragment)
        }

        // Reset inline parent container
        val newParent: ViewGroup = activity.findViewById(R.id.smallscreen_container)
        val movable = activity.findViewById<View>(R.id.movable_container)
        movable.detachFromParent()
        newParent.addView(movable)
    }
}
