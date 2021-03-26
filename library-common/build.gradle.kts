plugins {
    `android-library`
    kotlin("android")
    parcelize
}

androidLibraryConfig()

dependencies {
    implementation(Dependency.Fragment.runtime)
    implementation(Dependency.appCompat)
    implementation(Dependency.coreKtx)
    implementation(Dependency.multidex)
    implementation(Dependency.Kotlin.coroutines)

    testImplementation(Dependency.junit)
    testImplementation(Dependency.ArchCore.testing)
    testImplementation(Dependency.Kotlin.coroutinesTest)

    defaultAndroidTestDependencies()
}