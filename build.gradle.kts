import java.io.File

val gradleHomeDir = requireNotNull(gradle.gradleHomeDir) { "Gradle home directory not available" }
val gradleLibDir = gradleHomeDir.resolve("lib")

val kotlinCompilerClasspath = files(
    gradleLibDir.resolve("kotlin-compiler-embeddable-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-scripting-compiler-embeddable-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-scripting-compiler-impl-embeddable-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-daemon-embeddable-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-script-runtime-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-reflect-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-stdlib-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-scripting-common-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-scripting-jvm-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-scripting-jvm-host-2.0.21.jar"),
    gradleLibDir.resolve("kotlinx-coroutines-core-jvm-1.6.4.jar"),
    gradleLibDir.resolve("trove4j-1.0.20200330.jar"),
    gradleLibDir.resolve("annotations-24.0.1.jar")
)

val kotlinStdlibClasspath = files(
    gradleLibDir.resolve("kotlin-stdlib-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-reflect-2.0.21.jar"),
    gradleLibDir.resolve("kotlin-script-runtime-2.0.21.jar"),
    gradleLibDir.resolve("annotations-24.0.1.jar")
)

val coroutinesClasspath = files(
    gradleLibDir.resolve("kotlinx-coroutines-core-jvm-1.6.4.jar")
)

val junitClasspath = files(
    gradleLibDir.resolve("junit-4.13.2.jar"),
    gradleLibDir.resolve("hamcrest-core-1.3.jar")
)

val compileClasspath = kotlinStdlibClasspath + coroutinesClasspath
val mainSources = fileTree("app/src/main/kotlin") {
    include("**/*.kt")
    exclude("**/ui/**")
}
val testSources = fileTree("app/src/test/kotlin") { include("**/*.kt") }

val mainOutput = layout.buildDirectory.dir("classes/kotlin/main")
val testOutput = layout.buildDirectory.dir("classes/kotlin/test")

val testClassNames = providers.provider {
    fileTree("app/src/test/kotlin") { include("**/*Test.kt") }
        .files
        .map { file ->
            val relativePath = file.relativeTo(file("app/src/test/kotlin")).path
            relativePath.removeSuffix(".kt").replace(File.separatorChar, '.')
        }
}

tasks.register("clean") {
    group = "build"
    description = "Deletes the generated build outputs."
    doLast {
        delete(layout.buildDirectory)
    }
}

tasks.register<JavaExec>("compileKotlin") {
    group = "build"
    description = "Compiles main Kotlin sources using the bundled Kotlin compiler."
    inputs.files(mainSources)
    outputs.dir(mainOutput)
    mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
    classpath = kotlinCompilerClasspath
    doFirst {
        val outputDir = mainOutput.get().asFile
        outputDir.mkdirs()
        val argsList = mutableListOf("-no-stdlib", "-no-reflect", "-d", outputDir.absolutePath)
        if (!compileClasspath.isEmpty) {
            argsList += listOf("-classpath", compileClasspath.asPath)
        }
        argsList += mainSources.files.map { it.absolutePath }
        args = argsList
    }
}

tasks.register<JavaExec>("compileTestKotlin") {
    group = "build"
    description = "Compiles test Kotlin sources using the bundled Kotlin compiler."
    dependsOn("compileKotlin")
    inputs.files(testSources)
    outputs.dir(testOutput)
    mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
    classpath = kotlinCompilerClasspath
    doFirst {
        val outputDir = testOutput.get().asFile
        outputDir.mkdirs()
        val mainOutputDir = mainOutput.get().asFile
        val classpathEntries = mutableListOf<File>()
        classpathEntries += compileClasspath.files
        classpathEntries += mainOutputDir
        classpathEntries += junitClasspath.files
        val argsList = mutableListOf(
            "-no-stdlib", "-no-reflect",
            "-d", outputDir.absolutePath,
            "-classpath", classpathEntries.joinToString(File.pathSeparator) { it.absolutePath }
        )
        argsList += testSources.files.map { it.absolutePath }
        args = argsList
    }
}

tasks.register<JavaExec>("test") {
    group = "verification"
    description = "Runs JUnit-based unit tests."
    dependsOn("compileTestKotlin")
    mainClass.set("org.junit.runner.JUnitCore")
    doFirst {
        val runtimeClasspath = mutableListOf<File>()
        runtimeClasspath += compileClasspath.files
        runtimeClasspath += junitClasspath.files
        runtimeClasspath += mainOutput.get().asFile
        runtimeClasspath += testOutput.get().asFile
        classpath = files(runtimeClasspath)
        args = testClassNames.get()
    }
}

val check = tasks.register("check") {
    group = "verification"
    description = "Alias for running all verification tasks."
    dependsOn("test")
}

tasks.register("build") {
    group = "build"
    description = "Compiles sources and runs all tests."
    dependsOn("compileKotlin", "test")
}
