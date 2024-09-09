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
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

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

include("dht")
include("dht:core:model")
include("dht:core:service")
include("dht:core:grpc")
include("dht:server")
include("dht:client")

include("sample-vault")
include("sample-vault:core:model")
include("sample-vault:core:domain")
include("sample-vault:core:data")
include("sample-vault:java-app")
