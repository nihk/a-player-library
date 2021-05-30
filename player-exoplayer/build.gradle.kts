plugins {
    `android-library`
    kotlin("android")
}

androidLibraryConfig()

dependencies {
    implementation(project(Modules.Player.common))

    implementation(Dependencies.appCompat)
    implementation(Dependencies.material)
    implementation(Dependencies.coreKtx)
    implementation(Dependencies.constraintLayout)
    implementation(Dependencies.multidex)
    implementation(Dependencies.Kotlin.coroutines)
    implementation(Dependencies.ExoPlayer.runtime) // Consider making this api() for easier Exception type access

    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.ArchCore.testing)
    testImplementation(Dependencies.Kotlin.coroutinesTest)
    testImplementation(Dependencies.ExoPlayer.testUtils)

    androidTestImplementation(project(Modules.Player.test))
    defaultAndroidTestDependencies()
    androidTestImplementation(Dependencies.OkHttp.mockWebServer)
}