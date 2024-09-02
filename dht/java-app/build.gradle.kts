plugins {
    alias(libs.plugins.convention.java.application)
    alias(libs.plugins.convention.kotlin.jvm)
}

dependencies {
    implementation(projects.dht.core.service)
    implementation(projects.dht.core.model)
    implementation(projects.dht.core.grpc)

    implementation(libs.google.gson)
}
