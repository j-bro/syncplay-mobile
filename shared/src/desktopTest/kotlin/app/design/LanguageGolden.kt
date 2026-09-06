package app.design

import app.i18n.Locales
import app.preferences.settings.GLOBAL_NETWORK
import app.preferences.settings.INROOM_NOTICES
import app.preferences.settings.INROOM_SYNC
import app.preferences.settings.SettingsCategoryBody
import app.preferences.settings.SettingsCategoryList
import app.preferences.settings.SETTINGS_ROOM
import app.home.components.PopupAPropos
import kotlin.test.Test

/**
 * The same screens in every language the app ships.
 *
 * German and Russian words are longer than English ones and Arabic is read the other way, so a
 * layout that only ever gets checked in English is a layout nobody has checked. These renders
 * fail when a translated label is clipped or ellipsised.
 */
class LanguageGolden {

    private val languages = listOf(
        Locales.En, Locales.Ar, Locales.De, Locales.Es,
        Locales.Fr, Locales.Pl, Locales.Ru, Locales.Zh,
    )

    @Test
    fun settingsRowsSurviveEveryLanguage() {
        for (language in languages) {
            for (category in listOf(INROOM_SYNC, INROOM_NOTICES, GLOBAL_NETWORK)) {
                DesignHarness.render(
                    name = "lang-${category.key}",
                    widthDp = 360,
                    heightDp = 2200,
                    language = language,
                ) { SettingsCategoryBody(category) }.assertAllTextFits()
            }
        }
    }

    @Test
    fun theRoomsCategoryGridSurvivesEveryLanguage() {
        for (language in languages) {
            DesignHarness.render(
                name = "lang-room-categories",
                widthDp = 320,
                heightDp = 600,
                language = language,
            ) { SettingsCategoryList(SETTINGS_ROOM, columns = 2) {} }.assertAllTextFits()
        }
    }

    @Test
    fun theAboutPageSurvivesEveryLanguage() {
        for (language in languages) {
            DesignHarness.render(
                name = "lang-about",
                widthDp = 360,
                heightDp = 900,
                language = language,
            ) {
                PopupAPropos.AboutBody(
                    updateResult = null,
                    updateChecking = false,
                    onCheckUpdate = {},
                    onOpenUri = {},
                    onLicences = {},
                    onWatchAlone = {},
                )
            }.assertAllTextFits()
        }
    }
}
