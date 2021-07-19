package nick.sample.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nick.sample.R
import nick.sample.databinding.ListFragmentBinding
import nick.sample.databinding.PlayerItemBinding
import player.ui.common.PlayerArguments
import player.ui.inline.InlinePlaybackUi
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

class ListFragment : Fragment(R.layout.list_fragment) {
    private var binding: ListFragmentBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ListFragmentBinding.bind(view)

        val playingPositions = savedInstanceState?.getIntegerArrayList(KEY_PLAYING_POSITIONS)
            ?: mutableListOf()
        val adapter = Adapter(playingPositions)
        requireBinding().recyclerView.adapter = adapter

        adapter.submitList(items)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntegerArrayList(
            KEY_PLAYING_POSITIONS,
            ArrayList((requireBinding().recyclerView.adapter as Adapter).playingPositions)
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
                uuid = UUID.randomUUID()
            )
        }

        private const val KEY_PLAYING_POSITIONS = "playing_positions"
    }
}

data class PlayerItem(
    val uri: String,
    val color: Int,
    val uuid: UUID
)

class PlayerItemViewHolder(
    val binding: PlayerItemBinding,
    val playingPositions: MutableList<Int>
) : RecyclerView.ViewHolder(binding.root) {
    private var item: PlayerItem? = null

    init {
        binding.container.setOnClickListener {
            play()
        }
    }

    fun bind(item: PlayerItem) {
        unbind()
        this.item = item
        binding.container.background = ColorDrawable(item.color)
        if (bindingAdapterPosition in playingPositions) {
            // Restore state
            playInternal()
        }
    }

    fun unbind() {
        binding.play.isVisible = true
        binding.libraryView.stop()
    }

    private fun play() {
        playingPositions += bindingAdapterPosition
        playInternal()
    }

    private fun playInternal() {
        val item = requireNotNull(item)
        binding.libraryView.play(item.toPlayerArguments(), item.uuid)
        binding.play.isVisible = false
    }

    private fun PlayerItem.toPlayerArguments(): PlayerArguments {
        return PlayerArguments(
            uri = uri,
            playbackUiFactory = InlinePlaybackUi.Factory::class.java
        )
    }
}

class Adapter(
    val playingPositions: MutableList<Int>
) : ListAdapter<PlayerItem, PlayerItemViewHolder>(PlayerItemDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerItemViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.player_item, parent, false)
            .let { view -> PlayerItemBinding.bind(view) }
            .let { binding -> PlayerItemViewHolder(binding, playingPositions) }
    }

    override fun onBindViewHolder(holder: PlayerItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewDetachedFromWindow(holder: PlayerItemViewHolder) {
        playingPositions -= holder.bindingAdapterPosition
        holder.unbind()
    }
}

object PlayerItemDiffCallback : DiffUtil.ItemCallback<PlayerItem>() {
    override fun areItemsTheSame(oldItem: PlayerItem, newItem: PlayerItem): Boolean {
        return oldItem.uuid == oldItem.uuid
    }

    override fun areContentsTheSame(oldItem: PlayerItem, newItem: PlayerItem): Boolean {
        return oldItem == newItem
    }
}
