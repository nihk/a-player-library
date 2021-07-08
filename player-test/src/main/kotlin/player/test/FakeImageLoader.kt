package player.test

import android.widget.ImageView
import player.common.ImageLoader

class FakeImageLoader : ImageLoader {
    private val loadedUris = mutableListOf<String>()

    override fun load(imageView: ImageView, uri: String) {
        loadedUris += uri
    }
}
