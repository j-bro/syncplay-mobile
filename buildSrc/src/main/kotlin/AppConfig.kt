import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import java.io.File
import java.util.Properties

/**
 * Build helpers that are not app identity. App identity (name, version, bundle ids) is
 * declared once in the root kiteConfig { } block and read back through the kiteConfig
 * accessor; nothing here duplicates it. What lives here: the flavor flag, signing-secret
 * loading, native build data, brand colors, and the custom propagators.
 */
object AppConfig {
    /* ── Shared framework name (not app identity; kiteConfig does not expose it) ─────────────────── */
    const val SHARED_MODULE_NAME = "shared"

    /**
     * Reads `<rootDir>/local.properties` (signing secrets) and returns the parsed [Properties].
     * The caller's `rootDir` must be passed explicitly: resolving via the JVM working directory
     * returns empty Properties when the Gradle daemon's CWD isn't the project root, which surfaces
     * as a misleading "SigningConfig missing storePassword" error at sign time.
     */
    fun localProperties(rootDir: File): Properties = Properties().apply {
        val file = File(rootDir, "local.properties")
        if (file.exists()) load(file.inputStream())
    }

    /** Compile-time default for the [exoOnly] flavor. Override at build time with
     *  `-PexoOnly=true` (or a line in gradle.properties), see [resolveExoOnly]. */
    const val exoOnly = false

    /**
     * Resolves the [exoOnly] flavor flag, letting it be overridden from the command line
     * or gradle.properties (`-PexoOnly=true`) without editing source. This is what lets a
     * reproducible-build setup (e.g. IzzyOnDroid) select the exo-only variant, which ships no
     * native player library, via a plain Gradle invocation. Both the
     * build logic (androidApp) and the EXOPLAYER_ONLY BuildConfig field (shared) must read
     * through here so the build and the app code never disagree. Falls back to [exoOnly].
     */
    fun resolveExoOnly(providers: ProviderFactory): Boolean =
        providers.gradleProperty("exoOnly").orNull?.toBooleanStrictOrNull() ?: exoOnly

    /* ── Trinity brand colors (SSOT for the logo gradient) ──────────────────────────────────────── */
    // The three stops that cover most of the logo's visible sail area, so the wordmark, the
    // launcher icon and the default theme all read as one object. The full five-stop field lives
    // in art/synkplay_logo_palette.md; change the logo art and these move with it.
    const val TRINITY_1 = 0xFF9879EF  // Gentle ultraviolet (logo stop 25%)
    const val TRINITY_2 = 0xFFC331D8  // Softened orchid-magenta (logo stop 55%)
    const val TRINITY_3 = 0xFFD86B75  // Dusty coral (logo stop 88%)

    /**
     * Every native library the libmpvkt AAR carries. The exoOnly flavor strips them at packaging
     * time so that build ships no native player, which IzzyOnDroid's reproducible build relies
     * on. Keep it equal to what the pinned libmpvkt version ships; verifyExoOnlyApk fails the
     * build if a player library reaches the APK anyway.
     */
    val libmpvNativeLibs = listOf(
        "libavcodec.so", "libavdevice.so", "libavfilter.so", "libavformat.so", "libavutil.so",
        "libswresample.so", "libswscale.so", "libmpv.so", "libmpvkt_jni.so", "libc++_shared.so",
    )

    /* ── Propagation: trinity colors → Android vector drawable gradients ────────────────────────── */
    /**
     * The two source rewrites this repo does for itself now live in PropagationTasks as real
     * Gradle tasks, with declared inputs and outputs, instead of running at configuration time on
     * every invocation.
     */
}
