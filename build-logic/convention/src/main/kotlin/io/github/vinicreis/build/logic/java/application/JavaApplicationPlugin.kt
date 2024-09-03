package io.github.vinicreis.build.logic.java.application

import io.github.vinicreis.build.logic.java.config.test.setupTests
import org.gradle.api.Plugin
import org.gradle.api.Project

class JavaApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins()
            setupTests()
        }
    }

    private fun Project.applyPlugins() {
        pluginManager.apply("application")
    }
}