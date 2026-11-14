// PomDaisy (iOS): the Android app's real Compose UI and its data layer (app/src/main/kotlin), compiled for iOS with
// Compose Multiplatform - the same sources, so the iPhone looks and behaves like the Android handheld. The app's
// files are copied in (never edited here); what Android / the JVM gives them that iOS doesn't comes from src/shims
// (classes under the same names, kept small) and src/iosMain.
plugins {
    kotlin("multiplatform") version "2.4.21"
    kotlin("plugin.compose") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.2"
}

val app = rootDir.resolve("../../app/src/main/kotlin/com/pokedaisy/app")
// The data layer (as ui-preview's data module) and the UI (as ui-preview's root module).
// The same files ui-preview compiles (its data module and its root module).
val appIncludes = (findProperty("appIncludes") as String?)?.split(",") ?: listOf(
    "companion/data/**", "companion/*.kt", "companion/i18n/**", "companion/coop/**", "FeatureFlags.kt", "MgbaCore.kt",
    "Prefs.kt", "cheats/Cheats.kt", "GbaControls.kt", "Hotkeys.kt", "GameVolume.kt",
    "companion/ui/**", "SettingsActivity.kt", "CoopSettingsPages.kt", "LibraryActivity.kt", "SavesLocation.kt",
    "CoverArtSync.kt", "CoverSync.kt", "DocsApi.kt", "DocsSearch.kt", "RomFolder.kt", "StorageAccess.kt", "SetupScreen.kt",
    "AppUpdater.kt", "AppUpdate.kt", "GameSaves.kt", "RzipSave.kt", "SaveBackups.kt", "GameInfo.kt", "LibraryOrder.kt",
    "RomSupportRequest.kt", "RomIntake.kt", "BestEffortShare.kt", "SaveStates.kt", "RomIdentity.kt", "RomArchive.kt",
    "CompanionSupport.kt", "CoverPicker.kt", "SteamGridDbGames.kt", "SteamGridDbClient.kt", "GameTitles.kt",
    "SaveEditorActivity.kt", "SaveFileEdits.kt", "BpsPatch.kt", "RomPatcher.kt", "RomPatchFlow.kt", "RomCrcs.kt",
    "ScreenScraperClient.kt", "ScreenScraperUi.kt", "GameMedia.kt", "LauncherScreen.kt",
    "achievements/**", "overlay/Overlays.kt", "overlay/OverlayLabels.kt",
)
val appExcludes = (findProperty("appExcludes") as String?)?.split(",") ?: emptyList()

val prepSrc = tasks.register<Sync>("prepSrc") {
    from(app) { appIncludes.forEach { include(it) }; appExcludes.forEach { exclude(it) }; jniFiles.forEach { exclude(it) } }
    into(layout.buildDirectory.dir("appsrc"))
    // kotlin.jvm is a default import on the JVM only: its annotations spelled out (the copies only).
    // Dispatchers.IO is an extension to import on Kotlin/Native; LinkedHashMap is final there (java.util's shim is open);
    // java.lang.Long's statics are java.lang.JLong; String(CharArray) & co. are deprecated as errors here.
    filter { line: String ->
        var l = line.replace(Regex("@(JvmStatic|JvmField|JvmOverloads|JvmName|JvmInline|JvmSuppressWildcards)\\b"), "@kotlin.jvm.$1")
            .replace("object : LinkedHashMap<", "object : java.util.LinkedHashMap<")
            .replace("java.lang.Long.", "java.lang.JLong.")
        if (l.startsWith("package ")) l = "@file:Suppress(\"DEPRECATION_ERROR\")\n" + l + "\nimport kotlinx.coroutines.IO"
        l
    }
    inputs.property("filterVersion", 3)
}

// The app's JNI classes (MgbaCore: the emulator, RaNative: RetroAchievements) with their `external fun`s given stub bodies
// for now; they get bound to the iOS cores (ios/core, ios/ds, ios/melonds) through cinterop step by step.
val jniFiles = listOf("MgbaCore.kt", "achievements/RaNative.kt")
val stubJni = tasks.register("stubJni") {
    val srcs = jniFiles.map { app.resolve(it) }
    inputs.files(srcs)
    val outDir = layout.buildDirectory.dir("appsrc-jni")
    outputs.dir(outDir)
    doLast {
        for (rel in jniFiles) {
            val text = app.resolve(rel).readText()
                .replace(Regex("System\\.loadLibrary\\(\"[^\"]*\"\\)"), "Unit")
                .replace(Regex("@JvmStatic\\s+"), "")
                .replace(Regex("external fun (\\w+)\\((.*?)\\)(\\s*:\\s*([\\w.<>?, ]+?))?(?=\\s*(\\n|//|/\\*|$))", RegexOption.DOT_MATCHES_ALL)) { m ->
                    val name = m.groupValues[1]
                    val args = m.groupValues[2]
                    val type = m.groupValues[4].trim()
                    val body = when {
                        type.isEmpty() -> "Unit"
                        type.endsWith("?") -> "null"
                        type == "Boolean" -> "false"
                        type == "Int" -> "0"
                        type == "Long" -> "0L"
                        type == "Float" -> "0f"
                        type == "Double" -> "0.0"
                        type == "String" -> "\"\""
                        type == "IntArray" -> "IntArray(0)"
                        type == "ByteArray" -> "ByteArray(0)"
                        type == "LongArray" -> "LongArray(0)"
                        type == "ShortArray" -> "ShortArray(0)"
                        type == "FloatArray" -> "FloatArray(0)"
                        type.startsWith("Array<") -> "emptyArray()"
                        else -> "throw UnsupportedOperationException(\"$name\")"
                    }
                    "fun $name($args)" + (if (type.isEmpty()) "" else ": $type") + " = $body"
                }
            val out = outDir.get().asFile.resolve(rel)
            out.parentFile.mkdirs()
            out.writeText("@file:Suppress(\"DEPRECATION_ERROR\")\n" + text)
        }
    }
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "PomDaisyCompose"
            isStatic = true
        }
    }
    sourceSets {
        // Everything is iOS code (iosMain): the shims reach the platform (posix, Foundation) directly.
        iosMain {
            kotlin.srcDir(layout.buildDirectory.dir("appsrc"))
            kotlin.srcDir(layout.buildDirectory.dir("appsrc-jni"))
            kotlin.srcDir("src/shims")
            kotlin.srcDir("src/ios")
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.animation)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }
    }
    compilerOptions {
        // -Xallow-kotlin-package: src/shims/kotlin adds what Kotlin/JVM's standard library has (String.format ...).
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-nowarn", "-Xallow-kotlin-package")
        suppressWarnings.set(true)
    }
}
tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach { dependsOn(prepSrc, stubJni) }
