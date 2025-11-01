// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    //id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

