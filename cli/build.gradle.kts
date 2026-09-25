// The command-line tool: run, fetch, submit, check and key. It depends on puzzles, never the reverse.
plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":puzzles"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("ec.cli.MainKt")
    applicationName = "ec"
}

tasks.named<JavaExec>("run") {
    // inputs/, answers/ and .env live at the repository root, not in cli/.
    workingDir = rootDir
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true }
}
