package player.ui

import kotlin.time.Duration

interface PlayerController {
    fun play()
    fun pause()
    fun seekRelative(duration: Duration)
    fun seekTo(duration: Duration)
}
