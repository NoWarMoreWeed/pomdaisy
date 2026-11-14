// PomDaisy (iOS), module appui: the Android app's real Compose UI and its data layer (app/src/main/kotlin), compiled for iOS with
// Compose Multiplatform - the same sources, so the iPhone looks and behaves like the Android handheld. The app's
// files are copied in (never edited here); what Android / the JVM gives them that iOS doesn't comes from src/shims
// (classes under the same names, kept small) and src/iosMain.
plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

val app = rootDir.resolve("../../app/src/main/kotlin/com/pokedaisy/app")
// The data layer (as ui-preview's data module) and the UI (as ui-preview's root module).
// The same files ui-preview compiles (its data module and its root module).
val appIncludes = (findProperty("appIncludes") as String?)?.split(",") ?: listOf("**/*.kt")
// What iOS can't run: Firebase's crash reports (src/shims has a stand-in that sends none).
val appExcludes = (findProperty("appExcludes") as String?)?.split(",") ?: listOf("CrashReports.kt", "PixelTypeface.kt")

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

// The app's JNI classes. On a Mac (the iOS build) MgbaCore's `external fun`s become calls into the Android app's own
// native glue (ios/native: its Java_* functions over a JNI of our own) through cinterop; elsewhere (a Linux check,
// where Kotlin/Native can't run cinterop for iOS) they get stub bodies, as RaNative (RetroAchievements) does for now.
val jniFiles = listOf("MgbaCore.kt", "achievements/RaNative.kt")
val bindNative = org.gradle.internal.os.OperatingSystem.current().isMacOsX || hasProperty("bindNative")
val nativeDir = rootDir.resolve("../native")

data class JniFun(val name: String, val params: List<Pair<String, String>>, val ret: String)
fun jniFuns(text: String): List<JniFun> =
    Regex("external fun (\\w+)\\((.*?)\\)(\\s*:\\s*([\\w.<>?]+))?", RegexOption.DOT_MATCHES_ALL).findAll(text).map { m ->
        val params = m.groupValues[2].split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { p ->
            val (n, t) = p.split(":", limit = 2).map { it.trim() }
            n to t
        }
        JniFun(m.groupValues[1], params, m.groupValues[4].trim())
    }.toList()

fun stubBody(name: String, type: String) = when {
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

fun cType(t: String) = when (t.removeSuffix("?")) {
    "Int" -> "jint"; "Long" -> "jlong"; "Boolean" -> "jboolean"; "Double" -> "jdouble"; "Float" -> "jfloat"; "Short" -> "jshort"
    "Byte" -> "jbyte"; "" -> "void"; else -> "jobject"
}

/** The call into the glue for one external fun, its arguments and result converted (ios/compose/src/ios/.../PdJni.kt). */
fun boundBody(cls: String, f: JniFun): String {
    val pre = StringBuilder()
    val post = StringBuilder()
    val args = f.params.mapIndexed { i, (n, t) ->
        when (t.removeSuffix("?")) {
            "Int", "Long", "Double", "Float" -> n
            "Boolean" -> "pdBool($n)"
            "String" -> { pre.append("val j$i = pdString($n); "); post.append("pd_jni_free(j$i); "); "j$i" }
            "ByteArray" -> { pre.append("val j$i = pdBytes($n); "); post.append("pdBytesBack(j$i, $n); pd_jni_free(j$i); "); "j$i" }
            "IntArray" -> { pre.append("val j$i = pdInts($n); "); post.append("pdIntsBack(j$i, $n); pd_jni_free(j$i); "); "j$i" }
            "ShortArray" -> { pre.append("val j$i = pdShorts($n); "); post.append("pdShortsBack(j$i, $n); pd_jni_free(j$i); "); "j$i" }
            "Array<String>" -> { pre.append("val j$i = pdStrings($n); "); post.append("pd_jni_free(j$i); "); "j$i" }
            else -> "null"
        }
    }
    val call = "Java_${cls.replace('.', '_')}_${f.name.replace("_", "_1")}(pdEnv, null" + args.joinToString("") { ", $it" } + ")"
    val conv = when (f.ret.removeSuffix("?")) {
        "" -> call
        "Int", "Long", "Double", "Float" -> call
        "Boolean" -> "($call).toInt() != 0"
        "String" -> "pdTakeString($call)" + if (f.ret.endsWith("?")) "" else " ?: \"\""
        "ByteArray" -> "pdTakeBytes($call)" + if (f.ret.endsWith("?")) "" else " ?: ByteArray(0)"
        "IntArray" -> "pdTakeInts($call)" + if (f.ret.endsWith("?")) "" else " ?: IntArray(0)"
        "ShortArray" -> "pdTakeShorts($call)" + if (f.ret.endsWith("?")) "" else " ?: ShortArray(0)"
        "ByteBuffer", "java.nio.ByteBuffer" -> "pdTakeBuffer($call)"
        else -> "run { $call; null }"
    }
    return "run { ${pre}try { $conv } finally { $post} }"
}

val genJni = tasks.register("genJni") {
    inputs.files(jniFiles.map { app.resolve(it) })
    inputs.property("bind", bindNative)
    val outDir = layout.buildDirectory.dir("appsrc-jni")
    val glueDir = layout.buildDirectory.dir("glue")
    outputs.dirs(outDir, glueDir)
    doLast {
        val header = StringBuilder("/* The Android app's JNI glue functions MgbaCore calls (generated from MgbaCore.kt by ios/compose's genJni). */\n#include <jni.h>\n")
        for (rel in jniFiles) {
            val src = app.resolve(rel).readText()
            val bind = bindNative && rel == "MgbaCore.kt"
            val cls = if (rel == "MgbaCore.kt") "com.pokedaisy.app.MgbaCore" else "com.pokedaisy.app.achievements.RaNative"
            val funs = jniFuns(src).associateBy { it.name }
            if (bind) for (f in funs.values) {
                header.append("${cType(f.ret)} Java_${cls.replace('.', '_')}_${f.name.replace("_", "_1")}(JNIEnv *, jclass")
                f.params.forEach { (_, t) -> header.append(", ${cType(t)}") }
                header.append(");\n")
            }
            val text = src
                .replace(Regex("System\\.loadLibrary\\(\"[^\"]*\"\\)"), "Unit")
                .replace(Regex("@JvmStatic\\s+"), "")
                .replace(Regex("external fun (\\w+)\\((.*?)\\)(\\s*:\\s*([\\w.<>?]+))?", RegexOption.DOT_MATCHES_ALL)) { m ->
                    val name = m.groupValues[1]
                    val type = m.groupValues[4].trim()
                    val body = if (bind) boundBody(cls, funs.getValue(name)) else stubBody(name, type)
                    "fun $name(${m.groupValues[2]})" + (if (type.isEmpty()) "" else ": $type") + " = $body"
                }
            val imports = if (bind) "\nimport pdglue.*\nimport com.pokedaisy.ios.*" else ""
            val out = outDir.get().asFile.resolve(rel)
            out.parentFile.mkdirs()
            out.writeText("@file:Suppress(\"DEPRECATION_ERROR\")\n@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)\n" +
                text.replaceFirst(Regex("(?m)^(package .*)$"), "$1" + imports.replace("$", "\\$")))
        }
        val g = glueDir.get().asFile
        g.mkdirs()
        g.resolve("pd_glue.h").writeText(header.toString())
        g.resolve("pdglue.def").writeText("headers = pd_jni.h pd_glue.h\npackage = pdglue\n" +
            "compilerOpts = -I${nativeDir.resolve("include").absolutePath} -I${nativeDir.absolutePath} -I${g.absolutePath}\n")
    }
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        if (bindNative) target.compilations["main"].cinterops.create("pdglue") {
            definitionFile.set(layout.buildDirectory.file("glue/pdglue.def"))
        }
    }
    sourceSets {
        // Everything is iOS code (iosMain): the shims reach the platform (posix, Foundation) directly.
        iosMain {
            kotlin.srcDir(layout.buildDirectory.dir("appsrc"))
            kotlin.srcDir(layout.buildDirectory.dir("appsrc-jni"))
            kotlin.srcDir(rootDir.resolve("src/shims"))
            if (bindNative) kotlin.srcDir(rootDir.resolve("src/iosBound"))
            dependencies {
                api(compose.runtime)
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
tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach { dependsOn(prepSrc, genJni) }
tasks.matching { it.name.startsWith("cinteropPdglue") }.configureEach { dependsOn(genJni) }
