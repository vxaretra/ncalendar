package ui.chat

import kotlinx.coroutines.delay

/** Generates the assistant's reply for a conversation. */
interface ChatEngine {
    /** Returns the bot reply for [history]; the last message is the one being answered. */
    suspend fun reply(history: List<ChatMessage>): String
}

/**
 * Placeholder engine that echoes the most recent message after a short delay, mimicking a
 * "typing" pause. Kept behind [ChatEngine] so a real backend can replace it later.
 */
class EchoChatEngine(
    private val replyDelayMillis: Long = 500L,
) : ChatEngine {
    override suspend fun reply(history: List<ChatMessage>): String {
        delay(replyDelayMillis)
        return history.last().text
    }
}
