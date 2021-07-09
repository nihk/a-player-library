package player.ui.common

import androidx.fragment.app.FragmentActivity

interface CloseDelegate {
    fun onClose(activity: FragmentActivity)

    companion object {
        operator fun invoke(): CloseDelegate = Default()
    }

    private class Default : CloseDelegate {
        override fun onClose(activity: FragmentActivity) {
            activity.onBackPressed()
        }
    }
}
