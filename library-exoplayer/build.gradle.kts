plugins {
    `android-library`
    kotlin("android")
}

androidLibraryConfig()

dependencies {
    implementation(project(":library-common"))

    implementation(Dependency.appCompat)
    implementation(Dependency.material)
    implementation(Dependency.coreKtx)
    implementation(Dependency.constraintLayout)
    implementation(Dependency.multidex)
    implementation(Dependency.Kotlin.coroutines)
    implementation(Dependency.ExoPlayer.runtime) // Might want to make this api() for easier Exception type access

    testImplementation(Dependency.junit)
    testImplementation(Dependency.ArchCore.testing)
    testImplementation(Dependency.Kotlin.coroutinesTest)
}