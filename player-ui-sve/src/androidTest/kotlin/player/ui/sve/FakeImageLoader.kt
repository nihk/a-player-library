package player.ui.sve

import android.widget.ImageView

class FakeImageLoader : ImageLoader {
    private val loadedUris = mutableListOf<String>()

    override fun load(imageView: ImageView, uri: String) {
        loadedUris += uri
    }
}
