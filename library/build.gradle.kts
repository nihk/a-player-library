plugins {
    `android-library`
    kotlin("android")
}

androidLibraryConfig()

dependencies {
    api(project(":library-common"))
    implementation(project(":library-ui"))

    implementation(Dependency.activity)
    implementation(Dependency.appCompat)
    implementation(Dependency.coreKtx)
    implementation(Dependency.vectorDrawable)
    implementation(Dependency.constraintLayout)
    implementation(Dependency.material)
    implementation(Dependency.multidex)
    implementation(Dependency.Fragment.runtime)

    testImplementation(Dependency.junit)

    defaultAndroidTestDependencies()
}