allprojects {
    afterEvaluate {
        group = "io.github.vinicreis.sample"
        version = libs.versions.app.get()
    }
}

tasks.register("clean") {
    group = "build"
    description = "Deletes all build directories"

    subprojects.forEach {
        it.tasks.findByName("clean")?.let { cleanTask ->
            dependsOn(cleanTask)
        }
    }
}
