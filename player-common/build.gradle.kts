plugins {
    `android-library`
    kotlin("android")
    parcelize
}

androidLibraryConfig()

dependencies {
    implementation(Dependencies.Fragment.runtime)
    implementation(Dependencies.appCompat)
    implementation(Dependencies.coreKtx)
    implementation(Dependencies.vectorDrawable)
    implementation(Dependencies.constraintLayout)
    implementation(Dependencies.recyclerView)
    implementation(Dependencies.multidex)
    implementation(Dependencies.Kotlin.coroutines)
    implementation(Dependencies.Lifecycle.runtime)

    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.ArchCore.testing)
    testImplementation(Dependencies.Kotlin.coroutinesTest)

    defaultAndroidTestDependencies()
}