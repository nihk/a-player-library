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
    implementation(Dependencies.Kotlin.coroutinesTest)
    implementation(Dependencies.junit)
    implementation(Dependencies.ArchCore.testing)
    implementation(Dependencies.Kotlin.coroutinesTest)
}