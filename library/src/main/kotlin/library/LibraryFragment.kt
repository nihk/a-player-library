package library

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import library.common.requireNotNull
import library.ui.Constants
import library.ui.PlayerFragment

class LibraryFragment : Fragment(R.layout.library_fragment) {

    private val url: String get() = requireArguments().getString(Constants.KEY_URL).requireNotNull()

    override fun onAttach(context: Context) {
        val libraryModule = LibraryModule()
        childFragmentManager.fragmentFactory = libraryModule.fragmentFactory
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.container, PlayerFragment::class.java, PlayerFragment.args(url))
                .commit()
        }
    }

    companion object {
        fun create(url: String): LibraryFragment {
            return LibraryFragment().apply {
                arguments = args(url)
            }
        }

        fun args(url: String): Bundle {
            return bundleOf(Constants.KEY_URL to url)
        }
    }
}