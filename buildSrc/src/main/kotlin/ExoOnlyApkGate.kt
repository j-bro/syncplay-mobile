import java.util.zip.ZipFile
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * The exoOnly promise, checked on the bytes: an exoOnly APK carries no native player library.
 * Runs after every exoOnly package task and opens the APKs it produced. The exclusion list in
 * androidApp/build.gradle.kts is what keeps the promise; this is what proves it.
 */
fun Project.registerExoOnlyApkGate() {
    val forbiddenPrefixes = listOf("libmpv", "libmpvkt_jni", "libav", "libsw", "libkitecodec", "libc++_shared")
    val gate = tasks.register("verifyExoOnlyApk") {
        group = "verification"
        description = "Fails if an exoOnly APK carries a native player library."
        val apks = fileTree(layout.buildDirectory.dir("outputs/apk/exoOnly")) { include("**/*.apk") }
        inputs.files(apks)
        doLast {
            val offenders = apks.files.flatMap { apk ->
                ZipFile(apk).use { zip ->
                    zip.entries().asSequence()
                        .map { it.name }
                        .filter { name ->
                            name.startsWith("lib/") &&
                                forbiddenPrefixes.any { name.substringAfterLast('/').startsWith(it) }
                        }
                        .map { "${apk.name}: $it" }
                        .toList()
                }
            }
            if (offenders.isNotEmpty()) {
                throw GradleException(
                    "exoOnly must ship no native player library, but the APK carries:\n" +
                        offenders.joinToString("\n") { "  $it" } +
                        "\nCheck AppConfig.libmpvNativeLibs against what the pinned libmpvkt version ships.",
                )
            }
            logger.lifecycle("[Synkplay] exoOnly APK carries no native player library")
        }
    }
    tasks.matching { it.name.startsWith("packageExoOnly") }.configureEach { finalizedBy(gate) }
}
