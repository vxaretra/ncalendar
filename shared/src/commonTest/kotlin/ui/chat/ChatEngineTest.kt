package ui.chat

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.runBlocking

class ChatEngineTest {

    private fun message(role: Role, text: String) = ChatMessage(
        id = Random.nextLong().toString(16),
        role = role,
        text = text,
        timestamp = Clock.System.now(),
    )

    @Test
    fun echoReturnsLastMessageTextAfterDelay() = runBlocking {
        val history = listOf(
            message(Role.User, "Hello"),
            message(Role.Bot, "Hi! How can I help?"),
            message(Role.User, "Echo this"),
        )

        val start = TimeSource.Monotonic.markNow()
        val reply = EchoChatEngine().reply(history)
        val elapsed = start.elapsedNow()

        assertEquals("Echo this", reply)
        assertTrue(elapsed >= 500.milliseconds, "expected a ~500ms delay, but took $elapsed")
    }
}
