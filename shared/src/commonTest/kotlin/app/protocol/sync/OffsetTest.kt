package app.protocol.sync

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Two conversions, one sign convention. Getting the sign wrong in one direction only shows up on
 * a viewer who set an offset, which is why it went unnoticed on every path that had it backwards.
 */
class OffsetTest {

    @Test
    fun `a local seek is announced in room time`() {
        // Our copy runs 10 s ahead, so our 40 s is the room's 30 s.
        assertEquals(30.0, localToRoomSeconds(localMs = 40_000L, offsetSeconds = 10.0))
        assertEquals(50.0, localToRoomSeconds(localMs = 40_000L, offsetSeconds = -10.0))
    }

    @Test
    fun `a room position becomes a local target`() {
        assertEquals(40_000.0, roomToLocalMs(roomMs = 30_000.0, offsetSeconds = 10.0))
        assertEquals(20_000.0, roomToLocalMs(roomMs = 30_000.0, offsetSeconds = -10.0))
    }

    @Test
    fun `no offset changes nothing`() {
        assertEquals(40.0, localToRoomSeconds(localMs = 40_000L, offsetSeconds = 0.0))
        assertEquals(40_000.0, roomToLocalMs(roomMs = 40_000.0, offsetSeconds = 0.0))
    }

    @Test
    fun `the two conversions undo each other`() {
        val offset = 7.5
        val local = 123_456L
        assertEquals(local.toDouble(), roomToLocalMs(localToRoomSeconds(local, offset) * 1000.0, offset))
    }
}
