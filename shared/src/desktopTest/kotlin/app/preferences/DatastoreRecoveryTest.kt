package app.preferences

import kotlin.io.path.createTempFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The store is read once, blocking, behind the splash. An unreadable file used to throw there on
 * every launch, with nothing on screen to say why and no way out but reinstalling.
 */
class DatastoreRecoveryTest {

    @AfterTest
    fun restore() = resetPreferencesForTesting()

    @Test
    fun `garbage on disk becomes empty preferences, not a crash`() {
        val file = createTempFile(suffix = ".preferences_pb").toFile()
        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9))

        resetPreferencesForTesting()
        datastore = createDataStore { file.absolutePath }

        assertEquals(0, datastoreStateFlow.value.asMap().size)
        assertNotNull(preferencesLoadFailure, "the reset has to be reportable, or nobody is told")
    }
}
