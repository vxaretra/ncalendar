package assistant

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarConfirmationHandlerTest {

    /** Waits until [CalendarConfirmationHandler.awaitDecision] has registered its request. */
    private suspend fun awaitPending(handler: CalendarConfirmationHandler): PendingConfirmation {
        var pending = handler.pending.value
        while (pending == null) {
            yield()
            pending = handler.pending.value
        }
        return pending
    }

    @Test
    fun approveResolvesDecisionAndClearsPending() = runBlocking {
        val handler = CalendarConfirmationHandler()

        val decision = async { handler.awaitDecision("addEvent", "Add \"Standup\"?") }

        val pending = awaitPending(handler)
        assertEquals("addEvent", pending.toolName)
        assertEquals("Add \"Standup\"?", pending.description)

        handler.approve()

        assertTrue(decision.await(), "approve() should resolve the decision to true")
        assertNull(handler.pending.value, "pending should be cleared after the decision")
    }

    @Test
    fun denyResolvesDecisionToFalse() = runBlocking {
        val handler = CalendarConfirmationHandler()

        val decision = async { handler.awaitDecision("deleteEvent", "Delete \"Lunch\"?") }

        awaitPending(handler)
        handler.deny()

        assertFalse(decision.await(), "deny() should resolve the decision to false")
        assertNull(handler.pending.value)
    }

    @Test
    fun resolvingWithoutPendingRequestIsANoop() {
        val handler = CalendarConfirmationHandler()
        handler.approve()
        handler.deny()
        assertNull(handler.pending.value)
    }
}
