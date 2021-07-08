package player.ui.sve

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import player.common.ImageLoader
import player.common.PlayerViewWrapper
import player.ui.sve.databinding.SveItemBinding

internal class SveViewHolder(
    private val binding: SveItemBinding,
    private val imageLoader: ImageLoader?
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(sveItem: SveItem) {
        imageLoader?.load(binding.preview, sveItem.imageUri)
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
