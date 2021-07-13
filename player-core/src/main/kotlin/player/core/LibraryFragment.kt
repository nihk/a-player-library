package player.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import player.core.databinding.LibraryFragmentBinding
import player.ui.common.PlayerArguments
import player.ui.common.toBundle
import player.ui.common.toPlayerArguments

class LibraryFragment : Fragment(R.layout.library_fragment) {
    private val playerArguments get() = requireArguments().toPlayerArguments()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = LibraryFragmentBinding.bind(view)
        binding.libraryView.play(playerArguments)
    }

    companion object {
        fun create(playerArguments: PlayerArguments): LibraryFragment {
            return LibraryFragment().apply {
                arguments = playerArguments.toBundle()
            }
        }
    }
}
