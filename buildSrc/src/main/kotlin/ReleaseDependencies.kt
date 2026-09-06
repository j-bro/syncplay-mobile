import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Every version number the build pins, read once from where each one actually lives: the
 * version catalog, gradle.properties, the Gradle wrapper, the Swift package lock, the CocoaPods
 * lock and the mpv build scripts. The CLAUDE.md version table and the release page's
 * dependency table both come from here, so the two cannot disagree.
 */
internal class ToolVersions(root: File) {
    val catalog: Map<String, String> = Regex("""^([A-Za-z0-9_-]+)\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
        .findAll(File(root, "gradle/libs.versions.toml").readText().substringAfter("[versions]").substringBefore("["))
        .associate { it.groupValues[1] to it.groupValues[2] }

    val props: Map<String, String> = Regex("""^([A-Za-z0-9_.-]+)\s*=\s*(.+)$""", RegexOption.MULTILINE)
        .findAll(File(root, "gradle.properties").readText())
        .associate { it.groupValues[1].trim() to it.groupValues[2].trim() }

    /** Gradle's own version lives in the wrapper, not the catalog. */
    val gradle: String = Regex("""gradle-([0-9.]+)-bin\.zip""")
        .find(File(root, "gradle/wrapper/gradle-wrapper.properties").readText())?.groupValues?.get(1) ?: "?"

    /** Swift packages by identity, from the workspace's Package.resolved. */
    val swift: Map<String, String> = File(root, "iosApp/iosApp.xcworkspace/xcshareddata/swiftpm/Package.resolved")
        .takeIf { it.isFile }?.readText()?.let { text ->
            Regex(""""identity"\s*:\s*"([^"]+)"[\s\S]*?"version"\s*:\s*"([^"]+)"""")
                .findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
        }.orEmpty()

    /** CocoaPods by name, from the first "- Name (version)" line of Podfile.lock. */
    val pods: Map<String, String> = File(root, "iosApp/Podfile.lock").takeIf { it.isFile }?.readText()?.let { text ->
        Regex("""^\s+- ([A-Za-z0-9_-]+) \(([^)]+)\)$""", RegexOption.MULTILINE)
            .findAll(text).map { it.groupValues[1] to it.groupValues[2].removePrefix("= ") }.distinctBy { it.first }.toMap()
    }.orEmpty()

    /** The `v_*` pins of the mpv build scripts. mpv, libass, dav1d and libplacebo are git checkouts with no pin. */
    val native: Map<String, String> = File(root, "buildscripts/include/depinfo.sh").takeIf { it.isFile }?.readText()?.let { text ->
        Regex("""^v_([A-Za-z0-9_]+)=(\S+)$""", RegexOption.MULTILINE)
            .findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
    }.orEmpty()
}

/** One row of the release page's dependency table. */
private class Row(val component: String, val where: String, val version: String?)

/**
 * The dependency table the release notes carry, as Markdown. Curated rather than a dump of the
 * catalog: the rows are what someone deciding whether to trust or debug the app wants to know,
 * which is the players, the network stack and the toolchain, not every AndroidX artifact.
 */
internal fun releaseDependencyTable(v: ToolVersions): String {
    fun c(key: String) = v.catalog[key]
    val ffmpeg = v.native["ci_ffmpeg"]?.let { " (FFmpeg $it" } ?: " (FFmpeg"
    val rows = listOf(
        Row("Kotlin", "Toolchain", c("kotlin")),
        Row("Compose Multiplatform", "Toolchain", c("compose-multiplatform")),
        Row("Android Gradle Plugin", "Toolchain", c("agp")),
        Row("Gradle", "Toolchain", v.gradle),
        Row("KSP", "Toolchain", c("ksp")),
        Row("Android NDK", "Toolchain", v.props["android.ndkVersion"]),
        Row("KiteConfig", "Toolchain", c("kiteconfig")),
        Row("KitePlayer, with KiteFFmpeg inside", "All platforms, the experimental engine", c("kiteplayer")),
        Row("kotlinx-coroutines", "All platforms", c("koroutines")),
        Row("kotlinx-serialization", "All platforms", c("kSerialization")),
        Row("kotlinx-datetime", "All platforms", c("datetime")),
        Row("atomicfu", "All platforms", c("atomicfu")),
        Row("Ktor", "All platforms", c("ktor")),
        Row("Ktorfit", "All platforms", c("ktorfit")),
        Row("DataStore", "All platforms", c("datastore")),
        Row("Lyricist", "All platforms", c("lyricist")),
        Row("Navigation 3", "All platforms", c("navigation3Runtime")),
        Row("Coil", "All platforms", c("coil")),
        Row("Haze", "All platforms", c("haze")),
        Row("MaterialKolor", "All platforms", c("materialkolor")),
        Row("KolorPicker", "All platforms", c("kolorpicker")),
        Row("FileKit", "All platforms", c("filekit")),
        Row("Kermit", "All platforms", c("kermit")),
        Row("Media3 / ExoPlayer", "Android", c("media3")),
        Row("mpv, built from source$ffmpeg, libass, dav1d, libplacebo)", "Android, full build only", "latest git at build time"),
        Row("Netty", "Android and desktop", c("netty")),
        Row("Conscrypt", "Android", c("conscrypt")),
        Row("NewPipe Extractor", "Android and desktop", c("newpipeExtractor")?.removePrefix("v")),
        Row("VLCKit", "iOS", v.pods["VLCKit"]),
        Row("SwiftNIO", "iOS", v.swift["swift-nio"]),
        Row("SwiftNIO SSL", "iOS", v.swift["swift-nio-ssl"]),
        Row("YouTubeKit", "iOS", v.swift["youtubekit"]),
        Row("detekt", "Build checks", c("detekt")),
        Row("kover", "Build checks", c("kover")),
    )
    return buildString {
        appendLine("| Component | Where | Version |")
        appendLine("|---|---|---|")
        rows.filter { it.version != null }.forEach { appendLine("| ${it.component} | ${it.where} | ${it.version} |") }
    }.trim()
}

/** `printDependencyTable`: the release workflow captures this into the GitHub release notes. */
fun Project.registerDependencyTableTask(): TaskProvider<*> = tasks.register("printDependencyTable") {
    group = "syncplay"
    description = "Prints the dependency table for the GitHub release notes as Markdown."
    outputs.upToDateWhen { false }
    doLast { println(releaseDependencyTable(ToolVersions(projectDir))) }
}
