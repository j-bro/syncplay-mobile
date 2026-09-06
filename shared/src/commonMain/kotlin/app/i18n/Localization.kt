package app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.intl.Locale
import cafe.adriel.lyricist.Lyricist

/**
 * The app's display language, held in one place.
 *
 * Screens read [strings]; anything outside a composable reads [Localization.strings]. Both come
 * from the same [Lyricist], so changing the language moves the whole app at once with no restart.
 *
 * The layout stays left to right in every language, Arabic included: only the words change.
 * That is a deliberate choice, not an oversight.
 */
object Localization {

    /** The one instance. Composables get it through [ProvideAppStrings] in the root. */
    val lyricist: Lyricist<AppStrings> = Lyricist(Locales.En, appStrings)

    /** Strings for code that is not a composable: the protocol, the engines, notifications. */
    val strings: AppStrings get() = lyricist.strings

    /** The language the device is set to, or English when it is one we do not ship. */
    fun deviceLanguage(): String = Locale.current.toLanguageTag()

    /**
     * Applies a saved preference. A blank value means "follow the device", which is what the
     * language setting stores when nothing is chosen.
     */
    fun apply(saved: String) {
        lyricist.languageTag = saved.ifBlank { deviceLanguage() }
    }
}

/** The strings of the current language. The whole subtree re-reads them when it changes. */
val strings: AppStrings
    @Composable @ReadOnlyComposable get() = LocalAppStrings.current
