plugins {
    alias(libs.plugins.convention.java.library)
    alias(libs.plugins.convention.kotlin.jvm)
}

dependencies {
    implementation(projects.dht.core.model)
}
