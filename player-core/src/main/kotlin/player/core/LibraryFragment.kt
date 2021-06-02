package player.core

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import player.ui.controller.PlayerFragment
import player.ui.shared.PlayerArguments
import player.ui.shared.toBundle
import player.ui.shared.toPlayerArguments

class LibraryFragment : Fragment(R.layout.library_fragment) {
    private val playerArguments get() = requireArguments().toPlayerArguments()
    private lateinit var fm: FragmentManager

    override fun onAttach(context: Context) {
        if (context is LibraryActivity) {
            fm = requireActivity().supportFragmentManager
        } else {
            val libraryModule = LibraryModule(requireActivity(), childFragmentManager, playerArguments)
            childFragmentManager.fragmentFactory = libraryModule.fragmentFactory
            fm = childFragmentManager
        }
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            fm.beginTransaction()
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
