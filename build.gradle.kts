plugins {
    // One Kotlin version for every module; each module applies it without repeating the version.
    alias(libs.plugins.kotlin.jvm) apply false
}
