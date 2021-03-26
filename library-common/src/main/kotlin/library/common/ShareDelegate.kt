package library.common

import androidx.fragment.app.FragmentActivity

interface ShareDelegate {
    fun share(activity: FragmentActivity, url: String)
}