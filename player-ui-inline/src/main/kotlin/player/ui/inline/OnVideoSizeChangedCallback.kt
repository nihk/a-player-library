package player.ui.inline

import androidx.activity.ComponentActivity
import player.common.VideoSize

interface OnVideoSizeChangedCallback {
    fun onVideoSizeChanged(videoSize: VideoSize, activity: ComponentActivity)
}
