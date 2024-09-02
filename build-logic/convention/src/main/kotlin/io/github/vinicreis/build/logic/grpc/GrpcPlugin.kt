package io.github.vinicreis.build.logic.grpc

import com.google.protobuf.gradle.ProtobufExtension
import io.github.vinicreis.build.logic.extension.implementation
import io.github.vinicreis.build.logic.extension.libs
import io.github.vinicreis.build.logic.extension.testImplementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class GrpcPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            ensureProtobufIsApplied()
            configureProtobufPlugins()
            applyDependencies()
        }
    }

    private fun Project.ensureProtobufIsApplied() {
        check(pluginManager.hasPlugin(Plugins.PUBSUB_PROTOBUF)) {
            "Make sure that the plugin \"pubsub-protobuf\" from version catalog is applied"
        }
    }

    private fun Project.configureProtobufPlugins() {
        extensions.configure<ProtobufExtension> {
            plugins {
                create("grpc") {
                    artifact = "io.grpc:protoc-gen-grpc-java:${libs.findVersion(Versions.GRPC_CORE).get()}"
                }
                create("grpckt") {
                    artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.findVersion(Versions.GRPC_KOTLIN).get()}:jdk8@jar"
                }
            }
        }
    }

    private fun Project.applyDependencies() {
        dependencies {
            implementation(libs.findLibrary(Libraries.GRPC_PROTOBUF).get())
            implementation(libs.findLibrary(Libraries.GRPC_KOTLIN_STUB).get())
            implementation(libs.findLibrary(Libraries.GRPC_OKHTTP).get())

            testImplementation(libs.findLibrary(Libraries.GRPC_PROTOBUF).get())
        }
    }

    companion object {
        private object Versions {
            const val GRPC_CORE = "grpc.core"
            const val GRPC_KOTLIN = "grpc.kotlin"
        }

        private object Plugins {
            const val PUBSUB_PROTOBUF = "io.github.vinicreis.build.convention.protobuf"
        }

        private object Libraries {
            const val GRPC_PROTOBUF = "grpc.protobuf"
            const val GRPC_KOTLIN_STUB = "grpc.kotlin.stub"
            const val GRPC_OKHTTP = "grpc-okhttp"
        }
    }
}