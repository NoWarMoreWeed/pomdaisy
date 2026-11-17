// PomDaisy (iOS): the framework the Xcode app links - the entry points Swift calls (src/framework), over module appui
// (the Android app's own Kotlin / Compose code and its shims). appui stays out of the framework's Objective-C header:
// only these few functions are exported to Swift.
plugins {
    kotlin("multiplatform") version "2.4.21"
    kotlin("plugin.compose") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.2"
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "PomDaisyCompose"
            isStatic = true
            // Release stack traces with function names, files and lines (the crash reports in Crash.swift / Main.kt).
            freeCompilerArgs += "-Xadd-light-debug=enable"
            binaryOption("sourceInfoType", "libbacktrace")
        }
    }
    sourceSets {
        iosMain {
            kotlin.srcDir("src/framework")
            dependencies {
                implementation(project(":appui"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material3)
            }
        }
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-nowarn")
        suppressWarnings.set(true)
    }
}
