package nick.sample.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import nick.sample.R
import nick.sample.databinding.PlayerFragmentBinding
import player.ui.common.PlayerArguments
import player.ui.common.toBundle
import player.ui.common.toPlayerArguments

class PlayerFragment : Fragment(R.layout.player_fragment) {
    private val playerArguments get() = requireArguments().toPlayerArguments()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = PlayerFragmentBinding.bind(view)
        binding.libraryView.initialize(SampleLibraryConfigurationFactory().create(requireActivity()))
        binding.libraryView.play(playerArguments)
    }

    companion object {
        fun create(playerArguments: PlayerArguments): PlayerFragment {
            return PlayerFragment().apply {
                arguments = playerArguments.toBundle()
            }
        }
    }
}
