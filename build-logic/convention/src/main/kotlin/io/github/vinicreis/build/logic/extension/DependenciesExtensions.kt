package io.github.vinicreis.build.logic.extension

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun DependencyHandlerScope.implementation(dependency: Provider<MinimalExternalModuleDependency>) {
    "implementation"(dependency)
}

internal fun DependencyHandlerScope.testImplementation(dependency: Provider<MinimalExternalModuleDependency>) {
    "testImplementation"(dependency)
}
