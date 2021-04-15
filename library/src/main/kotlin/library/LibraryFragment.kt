package library

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import library.common.PictureInPictureConfig
import library.common.requireNotNull
import library.ui.Constants
import library.ui.PlayerFragment

class LibraryFragment : Fragment(R.layout.library_fragment) {

    private val url: String get() = requireArguments().getString(Constants.KEY_URL).requireNotNull()
    private val pictureInPictureConfig: PictureInPictureConfig? get() = requireArguments().getParcelable(Constants.KEY_PIP_CONFIG)

    override fun onAttach(context: Context) {
        val libraryModule = LibraryModule()
        childFragmentManager.fragmentFactory = libraryModule.fragmentFactory
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.container, PlayerFragment::class.java, PlayerFragment.args(url, pictureInPictureConfig))
                .commit()
        }
    }

    companion object {
        fun create(
            url: String,
            pictureInPictureConfig: PictureInPictureConfig? = null
        ): LibraryFragment {
            return LibraryFragment().apply {
                arguments = args(url, pictureInPictureConfig)
            }
        }

        fun args(
            url: String,
            pictureInPictureConfig: PictureInPictureConfig? = null
        ): Bundle {
            return bundleOf(
                Constants.KEY_URL to url,
                Constants.KEY_PIP_CONFIG to pictureInPictureConfig
            )
        }
    }
}