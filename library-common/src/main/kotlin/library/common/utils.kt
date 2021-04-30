package library.common

import android.os.Build

const val TAG = "asdf"
val isMinOsForPip get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O