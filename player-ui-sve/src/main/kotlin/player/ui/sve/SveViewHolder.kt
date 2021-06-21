package player.ui.sve

import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import player.common.PlayerViewWrapper
import player.ui.sve.databinding.SveItemBinding

internal class SveViewHolder(
    private val binding: SveItemBinding,
    private val imageLoader: ImageLoader
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(sveItem: SveItem) {
        binding.preview.load(sveItem.imageUri, imageLoader)
    }

    fun attach(playerViewWrapper: PlayerViewWrapper) {
        val alreadyAttached = playerViewWrapper.view.parent == binding.playerContainer
        if (alreadyAttached) return

        playerViewWrapper.view.detachFromParent()
        binding.playerContainer.addView(playerViewWrapper.view)
    }

    private fun View.detachFromParent() {
        val parent = parent as? ViewGroup ?: return
        parent.removeView(this)
    }
}
