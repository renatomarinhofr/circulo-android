plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}

// Optional override keeps local verification artifacts outside the source delivery.
providers.gradleProperty("circuloBuildDir").orNull?.let { destination ->
    subprojects { layout.buildDirectory.set(file("$destination/${project.name}")) }
}
