allprojects {
    afterEvaluate {
        group = "io.github.vinicreis"
        version = libs.versions.app.get()
    }
}
