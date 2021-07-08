package player.common

import android.widget.ImageView

interface ImageLoader {
    fun load(imageView: ImageView, uri: String)
}
