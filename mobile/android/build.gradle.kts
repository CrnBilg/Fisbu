allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// E2E-001: file_picker'ın transitive bağımlılığı flutter_plugin_android_lifecycle,
// compileSdk 36+ istiyor; app modülü zaten compileSdk=36 (bkz. app/build.gradle.kts)
// ama plugin modülleri kendi build.gradle'larında flutter.compileSdkVersion (34)
// kullanıyor — bu, TÜM alt modülleri (plugin'ler dahil) 36'ya zorlar.
subprojects {
    afterEvaluate {
        val androidExt = project.extensions.findByName("android")
        if (androidExt is com.android.build.gradle.BaseExtension) {
            androidExt.compileSdkVersion(36)
        }
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
