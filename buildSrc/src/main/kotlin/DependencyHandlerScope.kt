import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.defaultAndroidTestDependencies(
    dependencyNotation: String = "androidTestImplementation"
) {
    dependencyNotation(Dependencies.Espresso.core)
    dependencyNotation(Dependencies.Espresso.contrib)
    dependencyNotation(Dependencies.AndroidTest.core)
    dependencyNotation(Dependencies.AndroidTest.coreKtx)
    dependencyNotation(Dependencies.AndroidTest.extJunit)
    dependencyNotation(Dependencies.AndroidTest.runner)
    dependencyNotation(Dependencies.AndroidTest.rules)
}