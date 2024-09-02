plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlin.jvm)
    implementation(libs.kotlin.serialization)
    implementation(libs.google.protobuf.plugin)
}

gradlePlugin {
    plugins {
        register("convention.java.application") {
            id = "io.github.vinicreis.build.convention.java.application"
            implementationClass = "io.github.vinicreis.build.logic.java.application.JavaApplicationPlugin"
        }

        register("convention.java.library") {
            id = "io.github.vinicreis.build.convention.java.library"
            implementationClass = "io.github.vinicreis.build.logic.java.library.JavaLibraryPlugin"
        }

        register("convention.kotlin.jvm") {
            id = "io.github.vinicreis.build.convention.kotlin.jvm"
            implementationClass = "io.github.vinicreis.build.logic.jvm.KotlinJvmPlugin"
        }

        register("convention.protobuf") {
            id = "io.github.vinicreis.build.convention.protobuf"
            implementationClass = "io.github.vinicreis.build.logic.protobuf.ProtobufPlugin"
        }

        register("convention.grpc") {
            id = "io.github.vinicreis.build.convention.grpc"
            implementationClass = "io.github.vinicreis.build.logic.grpc.GrpcPlugin"
        }
    }
}
