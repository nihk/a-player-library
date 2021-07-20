package nick.sample.configuration

import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentActivity
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import nick.sample.R
import player.ui.inline.OnFullscreenChangedCallback

class SampleOnFullscreenChangedCallback : OnFullscreenChangedCallback {
    override fun onFullscreenChanged(isFullscreen: Boolean, activity: FragmentActivity) {
        val constraintLayout = activity.findViewById<ConstraintLayout>(R.id.inline_container)
        val constraintSet = ConstraintSet()
        val layout = if (isFullscreen) {
            R.layout.fullscreen
        } else {
            R.layout.smallscreen
        }
        constraintSet.clone(activity, layout)
        val transition = ChangeBounds().apply {
            interpolator = DecelerateInterpolator()
            duration = 400L
        }
        TransitionManager.beginDelayedTransition(constraintLayout, transition)
        constraintSet.applyTo(constraintLayout)
    }
}
