plugins {
    `android-library`
    kotlin("android")
    parcelize
}

androidLibraryConfig()

dependencies {
    implementation(project(Modules.Player.common))
    implementation(project(Modules.Player.Ui.trackspicker))
    api(project(Modules.Player.Ui.common))

    implementation(Dependencies.activity)
    implementation(Dependencies.Fragment.runtime)
    implementation(Dependencies.appCompat)
    implementation(Dependencies.coreKtx)
    implementation(Dependencies.vectorDrawable)
    implementation(Dependencies.constraintLayout)
    implementation(Dependencies.material)
    implementation(Dependencies.multidex)
    implementation(Dependencies.Kotlin.coroutines)
    implementation(Dependencies.savedState)
    implementation(Dependencies.Lifecycle.runtime)

    testImplementation(project(Modules.Player.test))
    testImplementation(project(Modules.Player.Ui.test))
    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.ArchCore.testing)
    testImplementation(Dependencies.Kotlin.coroutinesTest)

    androidTestImplementation(project(Modules.Player.test))
    androidTestImplementation(project(Modules.Player.Ui.test))
    defaultAndroidTestDependencies()
    androidTestImplementation(Dependencies.Kotlin.coroutinesTest)
}