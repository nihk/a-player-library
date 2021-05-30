package nick.sample.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.fragment.fragment
import dagger.hilt.android.AndroidEntryPoint
import player.common.OnUserLeaveHintViewModel
import player.core.LibraryFragment
import nick.sample.R
import nick.sample.di.MainEntryPoint
import nick.sample.navigation.AppNavigation

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.main_activity) {
    private val viewModel by viewModels<OnUserLeaveHintViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val entryPoint = entryPoint<MainEntryPoint>()
        supportFragmentManager.fragmentFactory = entryPoint.fragmentFactory
        super.onCreate(savedInstanceState)
        createNavGraph(entryPoint.navController)
    }

    private fun createNavGraph(navController: NavController) {
        navController.graph = navController.createGraph(
            id = AppNavigation.id,
            startDestination = AppNavigation.main
        ) {
            fragment<MainFragment>(AppNavigation.main)
            fragment<LibraryFragment>(AppNavigation.library)
        }
    }

    override fun onUserLeaveHint() {
        viewModel.onUserLeaveHint()
    }
}