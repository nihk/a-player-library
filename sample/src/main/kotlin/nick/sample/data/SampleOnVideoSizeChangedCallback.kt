package nick.sample.data

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentActivity
import nick.sample.R
import player.common.VideoSize
import player.ui.inline.OnVideoSizeChangedCallback

class SampleOnVideoSizeChangedCallback : OnVideoSizeChangedCallback {
    override fun onVideoSizeChanged(videoSize: VideoSize, activity: FragmentActivity) {
        val constraintLayout = activity.findViewById<ConstraintLayout>(R.id.constraint_layout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.setDimensionRatio(R.id.smallscreen_container, "${videoSize.widthPx}:${videoSize.heightPx}")
        constraintSet.applyTo(constraintLayout)
    }
}
