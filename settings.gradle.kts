@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS

    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "dht-kotlin"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("dht:core:model")
include("dht:core:service")
include("dht:core:domain")
include("dht:core:grpc")
include("dht:java-app")
