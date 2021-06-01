plugins {
    `android-library`
    kotlin("android")
    parcelize
}

androidLibraryConfig()

dependencies {
    implementation(project(Modules.Player.common))

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
    implementation(Dependencies.coil)

    // https://twitter.com/ianhlake/status/1059604904795230209
    debugImplementation(Dependencies.Fragment.testing)

    testImplementation(project(Modules.Player.test))
    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.ArchCore.testing)
    testImplementation(Dependencies.Kotlin.coroutinesTest)

    androidTestImplementation(project(Modules.Player.test))
    defaultAndroidTestDependencies()
    androidTestImplementation(Dependencies.Kotlin.coroutinesTest)
}