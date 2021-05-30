/*
 * © Microsoft Corporation. All rights reserved.
 */

package player.common

import kotlin.time.Duration

data class SeekData(
    val position: Duration,
    val buffered: Duration,
    val duration: Duration
) {
    companion object {
        val INITIAL = SeekData(
            position = Duration.ZERO,
            buffered = Duration.ZERO,
            duration = Duration.ZERO,
        )
    }
}
