package player.ui.common

import android.os.Build

val isMinOsForPip get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
