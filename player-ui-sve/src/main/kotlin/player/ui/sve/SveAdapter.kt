package player.ui.sve

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import player.ui.sve.databinding.BlankBinding

class SveAdapter : ListAdapter<SveItem, SveViewHolder>(SveItemDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SveViewHolder {
        return LayoutInflater.from(parent.context)
            .let { inflater -> BlankBinding.inflate(inflater, parent, false) }
            .let { binding -> SveViewHolder(binding) }
    }

    override fun onBindViewHolder(holder: SveViewHolder, position: Int) = Unit
}
