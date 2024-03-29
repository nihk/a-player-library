plugins {
    `android-library`
    kotlin("android")
}

androidLibraryConfig()

dependencies {
    api(project(Modules.Player.common))
    implementation(project(Modules.Player.Ui.core))
    implementation(project(Modules.Player.Ui.common))

    implementation(Dependencies.activity)
    implementation(Dependencies.appCompat)
    implementation(Dependencies.coreKtx)
    implementation(Dependencies.vectorDrawable)
    implementation(Dependencies.constraintLayout)
    implementation(Dependencies.material)
    implementation(Dependencies.multidex)
    implementation(Dependencies.Fragment.runtime)

    testImplementation(Dependencies.junit)

    defaultAndroidTestDependencies()
}