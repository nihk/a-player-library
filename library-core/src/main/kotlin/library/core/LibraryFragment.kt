package library.core

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import library.common.PlayerArguments
import library.common.bundle
import library.common.toPlayerArguments
import library.ui.PlayerFragment

class LibraryFragment : Fragment(R.layout.library_fragment) {

    private val playerArguments get() = requireArguments().toPlayerArguments()

    override fun onAttach(context: Context) {
        val libraryModule = LibraryModule(this)
        childFragmentManager.fragmentFactory = libraryModule.fragmentFactory
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.container, PlayerFragment::class.java, playerArguments.bundle())
                .commit()
        }
    }

    companion object {
        fun create(playerArguments: PlayerArguments): LibraryFragment {
            return LibraryFragment().apply {
                arguments = playerArguments.bundle()
            }
        }
    }
}