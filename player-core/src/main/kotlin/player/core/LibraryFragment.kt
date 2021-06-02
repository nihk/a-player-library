package player.core

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import player.ui.controller.PlayerFragment
import player.ui.shared.PlayerArguments
import player.ui.shared.toBundle
import player.ui.shared.toPlayerArguments

class LibraryFragment : Fragment(R.layout.library_fragment) {

    private val playerArguments get() = requireArguments().toPlayerArguments()

    override fun onAttach(context: Context) {
        val libraryModule = LibraryModule(this, playerArguments)
        childFragmentManager.fragmentFactory = libraryModule.fragmentFactory
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.container, PlayerFragment::class.java, playerArguments.toBundle())
                .commit()
        }
    }

    companion object {
        fun create(playerArguments: PlayerArguments): LibraryFragment {
            return LibraryFragment().apply {
                arguments = playerArguments.toBundle()
            }
        }
    }
}