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
}

application {
    mainClass.set("ec.RunnerKt")
    applicationName = "ec"
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true }
}
