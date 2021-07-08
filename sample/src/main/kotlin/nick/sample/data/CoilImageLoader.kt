package nick.sample.data

import android.widget.ImageView
import coil.load
import player.common.ImageLoader

class CoilImageLoader : ImageLoader {
    override fun load(imageView: ImageView, uri: String) {
        imageView.load(uri)
    }
}
