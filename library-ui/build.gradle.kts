plugins {
    `android-library`
    kotlin("android")
    parcelize
}

androidLibraryConfig()

dependencies {
    implementation(project(":library-common"))

    implementation(Dependency.activity)
    implementation(Dependency.Fragment.runtime)
    implementation(Dependency.appCompat)
    implementation(Dependency.coreKtx)
    implementation(Dependency.vectorDrawable)
    implementation(Dependency.constraintLayout)
    implementation(Dependency.material)
    implementation(Dependency.multidex)
    implementation(Dependency.Kotlin.coroutines)

    // https://twitter.com/ianhlake/status/1059604904795230209
    debugImplementation(Dependency.Fragment.testing)

    testImplementation(project(":library-test"))
    testImplementation(Dependency.junit)
    testImplementation(Dependency.ArchCore.testing)
    testImplementation(Dependency.Kotlin.coroutinesTest)

    androidTestImplementation(project(":library-test"))
    defaultAndroidTestDependencies()
    androidTestImplementation(Dependency.Kotlin.coroutinesTest)
}