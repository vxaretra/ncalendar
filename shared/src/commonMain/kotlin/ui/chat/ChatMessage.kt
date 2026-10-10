package ui.chat

import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Who authored a [ChatMessage]. */
enum class Role { User, Bot }

/** A single message exchanged in the AI assistant popup. */
data class ChatMessage(
    val id: String,
    val role: Role,
    val text: String,
    val timestamp: Instant,
)

/** Unique, [Uuid]-based message id; used as the message list key. */
fun newChatMessageId(): String = Uuid.random().toString()
