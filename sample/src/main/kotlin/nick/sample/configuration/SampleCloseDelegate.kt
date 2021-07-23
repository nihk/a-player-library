package nick.sample.configuration

import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import nick.sample.R
import nick.sample.ui.PlayerActivity
import player.ui.common.CloseDelegate
import player.core.LibraryView

class SampleCloseDelegate : CloseDelegate {
    override fun onClose(activity: ComponentActivity) {
        if (activity is PlayerActivity) {
            activity.finish()
        } else {
            val libraryView: LibraryView? = activity.findViewById(R.id.library_view)
            if (libraryView?.isPlaying == true) {
                libraryView.stop()
            }
            activity as FragmentActivity
            activity.supportFragmentManager.commit {
                activity.supportFragmentManager.fragments.forEach { fragment ->
                    remove(fragment)
                }
            }
            activity.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }
}
