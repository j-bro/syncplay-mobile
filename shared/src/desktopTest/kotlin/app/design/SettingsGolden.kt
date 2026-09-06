package app.design

import androidx.compose.runtime.CompositionLocalProvider
import app.preferences.settings.GLOBAL_ADVANCED
import app.preferences.settings.GLOBAL_NETWORK
import app.preferences.settings.INROOM_CHAT_PROPERTIES
import app.preferences.settings.INROOM_NOTICES
import app.preferences.settings.INROOM_PLAYER_SETTINGS
import app.preferences.settings.INROOM_SYNC
import app.preferences.settings.LocalSettingsDensity
import app.preferences.settings.SettingsDensity
import app.preferences.settings.SETTINGS_GLOBAL
import app.preferences.settings.SETTINGS_ROOM
import app.preferences.settings.SettingsCategoryBody
import app.preferences.settings.SettingsCategoryList
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The settings console on the real categories, with the height budgets from
 * DESIGN/PREF_SYSTEM. A change that reintroduces a seven line row fails here.
 */
class SettingsGolden {

    @Test
    fun roomExplanationsRemainReadableOnNarrowScreens() {
        for ((name, category) in listOf("sync" to INROOM_SYNC, "player" to INROOM_PLAYER_SETTINGS, "notices" to INROOM_NOTICES)) {
            for (scale in listOf(1f, 2f)) {
                // The real host scrolls. Give the complete, expanded category enough room here
                // that the fixture's viewport does not clip its last rows at 200% text.
                val result = DesignHarness.render("settings-$name-explained", 320, heightDp = 5000, fontScale = scale) {
                    CompositionLocalProvider(LocalSettingsDensity provides SettingsDensity(showInlineExplanations = true)) {
                        SettingsCategoryBody(category)
                    }
                }
                assertTrue(result.contentHeightDp < 5000, "Expanded settings exceeded the render canvas")
                result.assertAllTextFits()
            }
        }
    }

    @Test
    fun categories() {
        val network = DesignHarness.render("settings-network", 360) { SettingsCategoryBody(GLOBAL_NETWORK) }
        val player = DesignHarness.render("settings-player", 360) { SettingsCategoryBody(INROOM_PLAYER_SETTINGS) }
        val chat = DesignHarness.render("settings-chat", 360) { SettingsCategoryBody(INROOM_CHAT_PROPERTIES) }
        val notices = DesignHarness.render("settings-notices", 360) { SettingsCategoryBody(INROOM_NOTICES) }
        DesignHarness.render("settings-advanced", 360) { SettingsCategoryBody(GLOBAL_ADVANCED) }
        DesignHarness.render("settings-network", 720) { SettingsCategoryBody(GLOBAL_NETWORK) }
        DesignHarness.render("settings-player", 360, fontScale = 1.3f) { SettingsCategoryBody(INROOM_PLAYER_SETTINGS) }
        DesignHarness.render("settings-categories", 360) { SettingsCategoryList(SETTINGS_GLOBAL) {} }

        // The room shows its categories two per row inside the side panel. Notices made that six,
        // so the grid fills evenly; the names still have to survive the half-width cell.
        for (w in listOf(320, 440)) {
            DesignHarness.render("settings-room-categories", w) {
                SettingsCategoryList(SETTINGS_ROOM, columns = 2) {}
            }.assertAllTextFits()
        }

        // 260dp until the encryption-required row joined the category; still one screen, no scroll.
        // 289dp: the rows grew when every control target went to the platform minimum of 48dp.
        assertTrue(network.contentHeightDp <= 295, "network category ${network.contentHeightDp}dp exceeds its 295dp budget")
        // 690dp until the room gained a rotation switch; still one screen on a phone in landscape.
        assertTrue(player.contentHeightDp <= 750, "player category ${player.contentHeightDp}dp exceeds its 750dp budget")
        assertTrue(chat.contentHeightDp <= 900, "chat category ${chat.contentHeightDp}dp exceeds its 900dp budget")
        assertTrue(notices.contentHeightDp <= 400, "notices category ${notices.contentHeightDp}dp exceeds its 400dp budget")
    }
}
