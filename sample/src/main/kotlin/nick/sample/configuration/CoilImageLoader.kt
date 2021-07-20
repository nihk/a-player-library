package nick.sample.configuration

import android.widget.ImageView
import coil.load
import player.ui.sve.ImageLoader

class CoilImageLoader : ImageLoader {
    override fun load(imageView: ImageView, uri: String) {
        imageView.load(uri)
    }
}
