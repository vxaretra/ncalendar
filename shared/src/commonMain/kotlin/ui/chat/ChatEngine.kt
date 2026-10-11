package ui.chat

import kotlinx.coroutines.flow.Flow

/**
 * Generates the assistant's reply for a conversation.
 *
 * Implementations stream progress as [ChatEvent]s: text segments, tool activity, and
 * failures. The flow is expected to end with [ChatEvent.Completed] (after
 * [ChatEvent.Error] when the turn failed).
 */
interface ChatEngine {
    /**
     * Starts a reply turn for [userMessage] in the conversation identified by [sessionId].
     * Implementations may keep conversation history keyed by [sessionId].
     */
    fun replyStream(userMessage: String, sessionId: String): Flow<ChatEvent>
}
