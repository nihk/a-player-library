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
            useIR = true
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
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/MANIFEST.MF",
                "META-INF/proguard/coroutines.pro",
                "META-INF/*.kotlin_module",
                "META-INF/io.netty.versions.properties",
                "about.html",
                "LICENSE.txt"
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
        "implementation"(Dependency.Kotlin.stdlib)
    }
}