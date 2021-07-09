package player.ui.sve

import android.widget.ImageView

interface ImageLoader {
    fun load(imageView: ImageView, uri: String)
}
