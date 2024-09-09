subprojects {
    tasks.configureEach {
        fun Project.parentsNames(): String =
            parent?.parentsNames().orEmpty() +
                    parent?.takeIf { it.name != rootProject.name }?.let { "${it.name}-" }.orEmpty()

        if (this is Jar) {
            archiveBaseName = "${parentsNames()}${project.name}"
        }
    }
}
