package nick.sample.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nick.sample.R
import nick.sample.databinding.ListFragmentBinding
import nick.sample.databinding.PlayerItemBinding
import player.ui.common.PlayerArguments
import player.ui.inline.InlinePlaybackUi
import kotlin.random.Random

class ListFragment : Fragment(R.layout.list_fragment) {
    private var binding: ListFragmentBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ListFragmentBinding.bind(view)

        val playingIds: MutableList<String> = savedInstanceState?.getStringArrayList(KEY_PLAYING_UUIDS)
            ?: mutableListOf()
        val adapter = Adapter(playingIds)
        requireBinding().recyclerView.adapter = adapter

        adapter.submitList(items)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(
            KEY_PLAYING_UUIDS,
            ArrayList((requireBinding().recyclerView.adapter as Adapter).playingIds)
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
            val color = Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            PlayerItem(
                uri = uris[uriIndex],
                color = color,
                id = "${uris[uriIndex]}-$index"
            )
        }

        private const val KEY_PLAYING_UUIDS = "playing_uuids"
    }
}

data class PlayerItem(
    val uri: String,
    val color: Int,
    val id: String
)

class PlayerItemViewHolder(
    val binding: PlayerItemBinding,
    val playingIds: MutableList<String>
) : RecyclerView.ViewHolder(binding.root) {
    var item: PlayerItem? = null
        private set

    init {
        binding.container.setOnClickListener {
            play()
        }
    }

    fun bind(item: PlayerItem) {
        unbind()
        this.item = item
        binding.container.background = ColorDrawable(item.color)
        if (item.id in playingIds) {
            // Restore state
            playInternal()
        }
    }

    fun unbind() {
        binding.libraryView.stop()
    }

    private fun play() {
        playingIds += item!!.id
        playInternal()
    }

    private fun playInternal() {
        val item = requireNotNull(item)
        binding.libraryView.play(item.toPlayerArguments())
    }

    private fun PlayerItem.toPlayerArguments(): PlayerArguments {
        return PlayerArguments(
            id = id,
            uri = uri,
            playbackUiFactory = InlinePlaybackUi.Factory::class.java
        )
    }
}

class Adapter(
    val playingIds: MutableList<String>
) : ListAdapter<PlayerItem, PlayerItemViewHolder>(PlayerItemDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerItemViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.player_item, parent, false)
            .let { view -> PlayerItemBinding.bind(view) }
            .let { binding -> PlayerItemViewHolder(binding, playingIds) }
    }

    override fun onBindViewHolder(holder: PlayerItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewDetachedFromWindow(holder: PlayerItemViewHolder) {
        playingIds -= holder.item!!.id
        holder.unbind()
    }
}

object PlayerItemDiffCallback : DiffUtil.ItemCallback<PlayerItem>() {
    override fun areItemsTheSame(oldItem: PlayerItem, newItem: PlayerItem): Boolean {
        return oldItem.id == oldItem.id
    }

    override fun areContentsTheSame(oldItem: PlayerItem, newItem: PlayerItem): Boolean {
        return oldItem == newItem
    }
}
