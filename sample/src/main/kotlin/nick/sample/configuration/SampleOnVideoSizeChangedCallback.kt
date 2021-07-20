package nick.sample.configuration

import androidx.fragment.app.FragmentActivity
import player.common.VideoSize
import player.ui.inline.OnVideoSizeChangedCallback

class SampleOnVideoSizeChangedCallback : OnVideoSizeChangedCallback {
    override fun onVideoSizeChanged(videoSize: VideoSize, activity: FragmentActivity) {
        // fixme: this needs to coordinate with fullscreen constraintset changing
//        val constraintLayout = activity.findViewById<ConstraintLayout>(R.id.inline_container)
//        val constraintSet = ConstraintSet()
//        constraintSet.clone(constraintLayout)
//        constraintSet.setDimensionRatio(R.id.movable_container, "${videoSize.widthPx}:${videoSize.heightPx}")
//        constraintSet.applyTo(constraintLayout)
    }
}
