// The contract between Playder and its source modules. Plain Kotlin on purpose: no Android, no
// Media3, no HTTP library, so a module can be built as its own APK and loaded at runtime against
// whatever Playder version is installed, as long as API_VERSION matches.
plugins {
    id("kotlin")
}

kotlin {
    jvmToolchain(21)
}
