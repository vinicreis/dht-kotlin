plugins {
    alias(libs.plugins.convention.java.library)
    alias(libs.plugins.convention.kotlin.jvm)
    alias(libs.plugins.convention.protobuf)
    alias(libs.plugins.convention.grpc)
}

dependencies {
    implementation(projects.dht.core.model)
    implementation(projects.dht.client)
    implementation(projects.sampleVault.core.model)

    implementation(libs.google.gson)
}
