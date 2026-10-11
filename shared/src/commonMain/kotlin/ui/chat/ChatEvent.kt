package ui.chat

/**
 * Events emitted by a [ChatEngine] while a reply is being produced. The chat UI consumes
 * them to append assistant text, surface tool activity, and show failures.
 */
sealed interface ChatEvent {

    /**
     * A segment of assistant-visible text. Segments of one conversation turn are appended
     * to the same bubble, so a segment is usually a paragraph rather than a keystroke.
     */
    data class TextChunk(val text: String) : ChatEvent

    /** A tool call has started. [args] is a readable rendering of the call arguments. */
    data class ToolCallStarted(val toolName: String, val args: String) : ChatEvent

    /** A tool call finished. [result] is the tool's result text. */
    data class ToolCallCompleted(val toolName: String, val result: String) : ChatEvent

    /** A failure (missing API key, network error, ...). */
    data class Error(val message: String) : ChatEvent

    /** The reply turn is over; no further events follow. */
    data object Completed : ChatEvent
}
