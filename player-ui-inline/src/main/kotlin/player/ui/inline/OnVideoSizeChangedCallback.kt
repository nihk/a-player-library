package player.ui.inline

import androidx.fragment.app.FragmentActivity
import player.common.VideoSize

interface OnVideoSizeChangedCallback {
    fun onVideoSizeChanged(videoSize: VideoSize, activity: FragmentActivity)
}
