package app.utils

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The log is exportable from settings, so anything in it is something the user can hand to a
 * stranger. A service key reaches it through more paths than can be found one at a time.
 */
class LogRedactionTest {

    @Test
    fun `known secrets are masked wherever they appear`() {
        val out = redactSecrets(
            "GET /api/v1/ABCDEFGH12345678/search?x=1 key=ABCDEFGH12345678",
            listOf("ABCDEFGH12345678"),
        )
        assertEquals("GET /api/v1/***/search?x=1 key=***", out)
    }

    @Test
    fun `short or blank secrets are ignored so nothing common is masked`() {
        assertEquals("abc", redactSecrets("abc", listOf("", "a")))
    }

    @Test
    fun `text with no secret in it is untouched`() {
        assertEquals("nothing to see", redactSecrets("nothing to see", listOf("ABCDEFGH12345678")))
    }
}
