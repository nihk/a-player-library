plugins {
    `android-library`
    kotlin("android")
}

androidLibraryConfig()

dependencies {
    implementation(project(Modules.Player.common))
    implementation(project(Modules.Player.Ui.common))
    implementation(project(Modules.Player.test))

    implementation(Dependencies.appCompat)
    implementation(Dependencies.material)
    implementation(Dependencies.coreKtx)
    implementation(Dependencies.constraintLayout)
    implementation(Dependencies.multidex)
    implementation(Dependencies.Kotlin.coroutines)
    defaultAndroidTestDependencies("implementation")
}