package player.common

import androidx.fragment.app.FragmentActivity

interface CloseDelegate {
    fun onClose(activity: FragmentActivity)
}
