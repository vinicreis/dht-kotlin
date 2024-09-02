package io.github.vinicreis.build.logic.jvm

import io.github.vinicreis.build.logic.extension.implementation
import io.github.vinicreis.build.logic.extension.libs
import io.github.vinicreis.build.logic.model.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinJvmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins()
            configureKotlin()
            dependencies()
            testDependencies()
        }
    }

    private fun Project.applyPlugins() {
        pluginManager.apply(Plugins.KOTLIN_JVM)
        pluginManager.apply(Plugins.KOTLIN_SERIALIZATION)
    }

    private fun Project.configureKotlin() {
        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(Constants.JVM_TARGET)

            compilerOptions {
                freeCompilerArgs.add("-Xcontext-receivers")
            }
        }
    }

    private fun Project.dependencies() {
        dependencies {
            implementation(libs.findLibrary(Libraries.KOTLINX_COROUTINES_CORE).get())
            implementation(libs.findLibrary(Libraries.KOTLINX_DATETIME).get())
            implementation(libs.findLibrary(Libraries.KOTLINX_SERIALIZATION_PROTOBUF).get())
        }
    }

    private fun Project.testDependencies() {
        dependencies {
            implementation(libs.findLibrary(Libraries.KOTLINX_COROUTINES_TEST).get())
        }
    }

    companion object {
        private object Plugins {
            const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
            const val KOTLIN_SERIALIZATION = "org.jetbrains.kotlin.plugin.serialization"
        }

        private object Libraries {
            const val KOTLINX_DATETIME = "kotlinx.datetime"
            const val KOTLINX_SERIALIZATION_PROTOBUF = "kotlinx.serialization.protobuf"
            const val KOTLINX_COROUTINES_CORE = "kotlinx.coroutines.core"
            const val KOTLINX_COROUTINES_TEST = "kotlinx.coroutines.test"
        }
    }
}