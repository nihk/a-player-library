plugins {
    `android-library`
    kotlin("android")
}

androidLibraryConfig()

dependencies {
    api(project(Modules.Player.common))
    implementation(project(Modules.Player.Ui.shared))
    implementation(project(Modules.Player.Ui.controller))

    implementation(Dependencies.activity)
    implementation(Dependencies.appCompat)
    implementation(Dependencies.coreKtx)
    implementation(Dependencies.vectorDrawable)
    implementation(Dependencies.constraintLayout)
    implementation(Dependencies.material)
    implementation(Dependencies.multidex)
    implementation(Dependencies.Fragment.runtime)
    implementation(Dependencies.coil)

    testImplementation(Dependencies.junit)

    defaultAndroidTestDependencies()
}