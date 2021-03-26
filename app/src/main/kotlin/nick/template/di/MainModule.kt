package nick.template.di

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.multibindings.IntoMap
import library.LibraryFragment
import nick.template.R
import nick.template.ui.AppFragmentFactory
import nick.template.ui.MainFragment

@Module
@InstallIn(ActivityComponent::class)
abstract class MainModule {

    companion object {
        @Provides
        fun navController(activity: Activity): NavController {
            val navHostFragment = (activity as AppCompatActivity).supportFragmentManager
                .findFragmentById(R.id.navHostContainer) as NavHostFragment
            return navHostFragment.navController
        }

        @Provides
        @IntoMap
        @FragmentKey(LibraryFragment::class)
        fun libraryFragment(): Fragment = LibraryFragment()
    }

    @Binds
    @IntoMap
    @FragmentKey(MainFragment::class)
    abstract fun mainFragment(mainFragment: MainFragment): Fragment

    @Binds
    abstract fun fragmentFactory(appFragmentFactory: AppFragmentFactory): FragmentFactory
}