package nick.sample.ui.list

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import nick.sample.R
import nick.sample.databinding.ListFragmentBinding
import kotlin.random.Random

class ListFragment : Fragment(R.layout.list_fragment) {
    private var binding: ListFragmentBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ListFragmentBinding.bind(view)

        val playingPositions: MutableList<Int> = savedInstanceState?.getIntegerArrayList(KEY_PLAYING_POSITIONS)
            ?: mutableListOf()
        val fullscreenPositions: MutableList<Int> = savedInstanceState?.getIntegerArrayList(KEY_FULLSCREEN_POSITIONS)
            ?: mutableListOf()
        val adapter = Adapter(playingPositions, fullscreenPositions.toMutableSet(), requireBinding().fullscreenContainer)
        requireBinding().recyclerView.adapter = adapter

        adapter.submitList(items)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val adapter = requireBinding().recyclerView.adapter as Adapter
        outState.putIntegerArrayList(
            KEY_PLAYING_POSITIONS,
            ArrayList(adapter.playingPositions)
        )
        outState.putIntegerArrayList(
            KEY_FULLSCREEN_POSITIONS,
            ArrayList(adapter.fullscreenPositions)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun requireBinding() = requireNotNull(binding)

    companion object {
        private val uris = listOf(
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            "file:///android_asset/waves.mp4",
            "https://bestvpn.org/html5demos/assets/dizzy.mp4"
        )

        private val items: List<PlayerItem> = List(30) { index ->
            val uriIndex = index % uris.size
            val randomColor = Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            PlayerItem(
                uri = uris[uriIndex],
                color = randomColor,
                id = "${uris[uriIndex]}-$index"
            )
        }

        private const val KEY_PLAYING_POSITIONS = "playing_positions"
        private const val KEY_FULLSCREEN_POSITIONS = "fullscreen_positions"
    }
}
