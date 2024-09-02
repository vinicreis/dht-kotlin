package io.github.vinicreis.build.logic.protobuf

import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.proto
import io.github.vinicreis.build.logic.extension.implementation
import io.github.vinicreis.build.logic.extension.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class ProtobufPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins()
            configureSourceSets()
            configureProtobuf()
            configureCopyTasks()
            applyDependencies()
        }
    }

    private fun Project.applyPlugins() {
        pluginManager.apply {
            apply(Plugins.GOOGLE_PROTOBUF)
        }
    }

    private fun Project.configureSourceSets() {
        extensions.configure<JavaPluginExtension> {
            sourceSets.named("main").configure {
                proto {
                    srcDir("src/main/proto")
                }
            }
        }
    }

    private fun Project.configureProtobuf() {
        extensions.configure<ProtobufExtension> {
            protoc {
                artifact = "com.google.protobuf:protoc:${libs.findVersion(Versions.PROTOBUF_COMPILER).get()}"
            }

            generateProtoTasks {
                all().forEach { task ->
                    task.plugins {
                        create("grpc")
                        create("grpckt")
                    }

                    task.builtins {
                        create("kotlin")
                    }
                }
            }
        }
    }

    private fun Project.configureCopyTasks() {
        tasks.withType<Copy> {
            filesMatching("**/*.proto") {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
        }
    }

    private fun Project.applyDependencies() {
        dependencies {
            implementation(libs.findLibrary(Libraries.GOOGLE_PROTOBUF).get())
        }
    }

    companion object {
        private object Plugins {
            const val GOOGLE_PROTOBUF = "com.google.protobuf"
        }

        private object Versions {
            const val PROTOBUF_COMPILER = "protobuf.compiler"
        }

        private object Libraries {
            const val GOOGLE_PROTOBUF = "google.protobuf.kotlin"
        }
    }
}