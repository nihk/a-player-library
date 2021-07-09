package player.ui.test

import player.ui.common.TimeFormatter
import kotlin.time.Duration

class FakeTimeFormatter : TimeFormatter {
    override fun playerTime(duration: Duration): String {
        return "6:00"
    }
}
