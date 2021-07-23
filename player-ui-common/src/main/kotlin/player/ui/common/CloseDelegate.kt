package player.ui.common

import androidx.activity.ComponentActivity

interface CloseDelegate {
    fun onClose(activity: ComponentActivity)

    companion object {
        operator fun invoke(): CloseDelegate = Default()
    }

    private class Default : CloseDelegate {
        override fun onClose(activity: ComponentActivity) {
            activity.onBackPressed()
        }
    }
}
