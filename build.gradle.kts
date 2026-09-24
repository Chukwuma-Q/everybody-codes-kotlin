plugins {
    kotlin("jvm") version "2.4.20"
    application
}

repositories { mavenCentral() }

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Experimental stdlib bits from 2.4.20: allDistinct(), allEqual(), etc.
        freeCompilerArgs.add("-opt-in=kotlin.ExperimentalStdlibApi")
    }
}

application {
    mainClass.set("ec.RunnerKt")
    applicationName = "ec"
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true }
}

tasks.named<JavaExec>("run") {
    // Solutions read their input from a file, but keep stdin usable anyway.
    standardInput = System.`in`
}
