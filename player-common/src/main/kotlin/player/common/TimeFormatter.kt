package player.common

import java.util.*
import kotlin.math.abs
import kotlin.time.Duration

interface TimeFormatter {
    fun playerTime(duration: Duration): String
}

class DefaultTimeFormatter(locale: Locale) : TimeFormatter {
    private val builder = StringBuilder()
    private val formatter = Formatter(builder, locale)

    override fun playerTime(duration: Duration): String {
        // Copied from com.google.android.exoplayer2.util.Util.getStringForTime
        var timeMs = duration.inWholeMilliseconds
        val prefix = if (timeMs < 0) "-" else ""
        timeMs = abs(timeMs)
        val totalSeconds: Long = (timeMs + 500) / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        builder.setLength(0)
        return if (hours > 0) formatter.format("%s%d:%02d:%02d", prefix, hours, minutes, seconds)
            .toString() else formatter.format("%s%02d:%02d", prefix, minutes, seconds).toString()
    }
}
