package nick.template.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import library.LibraryActivity
import library.LibraryFragment
import nick.template.R
import nick.template.databinding.MainFragmentBinding
import nick.template.navigation.AppNavigation
import javax.inject.Inject

class MainFragment @Inject constructor(
    private val navController: NavController
) : Fragment(R.layout.main_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = MainFragmentBinding.bind(view)
        val url = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"

        binding.toPlayerFragment.setOnClickListener {
            navController.navigate(AppNavigation.library, LibraryFragment.args(url))
        }

        binding.toPlayerActivity.setOnClickListener {
            LibraryActivity.start(view.context, url)
        }
    }
}