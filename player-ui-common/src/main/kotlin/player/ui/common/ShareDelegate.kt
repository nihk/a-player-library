package player.ui.common

import androidx.fragment.app.FragmentActivity

interface ShareDelegate {
    fun share(activity: FragmentActivity, uri: String)
}