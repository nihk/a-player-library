package player.ui.sve

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import player.common.PlayerViewWrapper
import player.ui.controller.detachFromParent
import player.ui.sve.databinding.SveItemBinding

internal class SveViewHolder(
    private val binding: SveItemBinding,
    private val imageLoader: ImageLoader,
    onClick: (View) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener(onClick)
    }

    fun bind(sveItem: SveItem) {
        imageLoader.load(binding.preview, sveItem.imageUri)
    }

    fun attach(playerViewWrapper: PlayerViewWrapper) {
        val alreadyAttached = playerViewWrapper.view.parent == binding.playerContainer
        if (alreadyAttached) return

        playerViewWrapper.view.detachFromParent()
        binding.playerContainer.addView(playerViewWrapper.view)
    }
}
