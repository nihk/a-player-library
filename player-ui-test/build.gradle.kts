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
    implementation(Dependencies.Espresso.core)
    implementation(Dependencies.Espresso.contrib)
    implementation(Dependencies.AndroidTest.core)
    implementation(Dependencies.AndroidTest.coreKtx)
    implementation(Dependencies.AndroidTest.extJunit)
    implementation(Dependencies.AndroidTest.runner)
    implementation(Dependencies.AndroidTest.rules)
}