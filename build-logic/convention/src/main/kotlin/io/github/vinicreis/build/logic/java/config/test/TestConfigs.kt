package io.github.vinicreis.build.logic.java.config.test

import io.github.vinicreis.build.logic.extension.libs
import io.github.vinicreis.build.logic.extension.testImplementation
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

private object Libraries {
    const val JUNIT_BOM = "junit.bom"
    const val JUNIT_JUPITER = "junit.jupiter"
    const val MOCKK = "mockk"
}

internal fun Project.setupTests() {
    configureTestDependencies()
    configureJUnit()
}

private fun Project.configureTestDependencies() {
    dependencies {
        testImplementation(platform(libs.findLibrary(Libraries.JUNIT_BOM).get()))
        testImplementation(libs.findLibrary(Libraries.JUNIT_JUPITER).get())
        testImplementation(libs.findLibrary(Libraries.MOCKK).get())
    }
}

private fun Project.configureJUnit() {
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
