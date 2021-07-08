package player.test

import player.common.TimeFormatter
import kotlin.time.Duration

class FakeTimeFormatter : TimeFormatter {
    override fun playerTime(duration: Duration): String {
        return "6:00"
    }
}
