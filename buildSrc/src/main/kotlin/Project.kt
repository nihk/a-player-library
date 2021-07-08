import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.androidAppConfig(extras: (BaseAppModuleExtension.() -> Unit) = {}) = androidConfig<BaseAppModuleExtension>().run {
    defaultConfig {
        buildToolsVersion(BuildVersion.buildTools)
        multiDexEnabled = true
    }

    buildFeatures {
        buildConfig = true
    }

    extras()
}

fun Project.androidLibraryConfig(extras: (LibraryExtension.() -> Unit) = {}) = androidConfig<LibraryExtension>().run {
    buildFeatures {
        buildConfig = false
    }

    extras()
}

private fun <T : BaseExtension> Project.androidConfig() = android<T>().apply {
    compileSdkVersion(BuildVersion.compileSdk)

    defaultConfig {
        minSdkVersion(BuildVersion.minSdk)
        targetSdkVersion(BuildVersion.targetSdk)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            consumerProguardFiles("consumer-rules.pro")
        }
    }

    buildFeatures.apply {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType(KotlinCompile::class.java) {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
            freeCompilerArgs = listOf(
                "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-Xuse-experimental=kotlin.time.ExperimentalTime",
                "-Xuse-experimental=kotlinx.coroutines.FlowPreview"
            )
        }
    }

    sourceSets {
        val sharedTestDir = "src/sharedTest/kotlin"
        getByName("main").java.srcDir("src/main/kotlin")
        getByName("test").java.srcDirs(
            "src/test/kotlin",
            sharedTestDir
        )
        getByName("androidTest").java.srcDirs(
            "src/androidTest/kotlin",
            sharedTestDir
        )
    }

    testOptions {
        animationsDisabled = true
    }

    packagingOptions {
        setExcludes(
            setOf(
                "about.html",
                "LICENSE.txt",
                "NOTICE.txt",
                "META-INF/**",
            )
        )
    }
}.also {
    defaultDependencies()
}

private fun <T : BaseExtension> Project.android(): T {
    @Suppress("UNCHECKED_CAST")
    return extensions.findByName("android") as T
}

fun Project.jvmConfig() {
    val sourceSets = extensions.getByName("sourceSets") as SourceSetContainer
    sourceSets["main"].java.srcDir("src/main/kotlin")

    defaultDependencies()
}

private fun Project.defaultDependencies() {
    dependencies {
        "implementation"(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
        "implementation"(Dependencies.Kotlin.stdlib)
    }
}