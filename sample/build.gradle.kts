plugins {
    `android-application`
    kotlin("android")
}

androidAppConfig {
    defaultConfig {
        minSdkVersion(26) // For sample only -- to support PiP
        applicationId = "nick.a_player_library"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(Modules.Player.core))
    implementation(project(Modules.Player.exoplayer))
    implementation(project(Modules.Player.mediaplayer))
    implementation(project(Modules.Player.Ui.default))
    implementation(project(Modules.Player.Ui.sve))

    implementation(Dependencies.activity)
    implementation(Dependencies.Fragment.runtime)
    implementation(Dependencies.appCompat)
    implementation(Dependencies.coreKtx)
    implementation(Dependencies.vectorDrawable)
    implementation(Dependencies.constraintLayout)
    implementation(Dependencies.material)
    implementation(Dependencies.multidex)

    debugImplementation(Dependencies.leakCanary)

    testImplementation(Dependencies.junit)
}
