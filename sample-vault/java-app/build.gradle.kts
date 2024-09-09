plugins {
    alias(libs.plugins.convention.java.application)
    alias(libs.plugins.convention.kotlin.jvm)
    alias(libs.plugins.convention.protobuf)
    alias(libs.plugins.convention.grpc)
}

application {
    applicationName = "dht-vault-sample"
    mainClass.set("io.github.vinicreis.dht.sample.vault.app.MainKt")
}

dependencies {
    implementation(projects.dht.core.model)
    implementation(projects.sampleVault.core.model)
    implementation(projects.sampleVault.core.domain)

    implementation(libs.google.gson)
}
