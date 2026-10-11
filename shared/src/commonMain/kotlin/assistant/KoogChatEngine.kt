package assistant

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import data.EventRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ui.chat.ChatEngine
import ui.chat.ChatEvent
import kotlin.time.Clock

/**
 * Chat engine backed by a Koog AI agent with [CalendarToolSet] tools, using OpenRouter as
 * the LLM provider.
 *
 * Each call to [replyStream] builds a fresh agent for the turn, then runs it while
 * forwarding agent lifecycle events as [ChatEvent]s: assistant text per LLM call, tool
 * activity as it happens. Write tools park inside [CalendarConfirmationHandler] until the
 * user approves or denies.
 *
 * Conversation history is shared across turns through [chatHistory], keyed by the session
 * id passed to [replyStream].
 */
class KoogChatEngine(
    private val apiKey: String?,
    private val eventRepository: EventRepository,
    private val confirmationHandler: CalendarConfirmationHandler,
    private val model: LLModel = OpenRouterModels.GPT4oMini,
) : ChatEngine {

    // Shared across the per-turn agents so the conversation survives between replies.
    private val chatHistory = InMemoryChatHistoryProvider()

    // Reused across turns: one HTTP-backed executor for the whole app session.
    // Resolving the key is deferred until the first real reply, so a missing key
    // surfaces as a chat error instead of crashing app startup.
    private val executor by lazy {
        MultiLLMPromptExecutor(OpenRouterLLMClient(apiKey = requireNotNull(apiKey)))
    }

    override fun replyStream(userMessage: String, sessionId: String): Flow<ChatEvent> = channelFlow {
        if (apiKey.isNullOrBlank()) {
            trySend(
                ChatEvent.Error(
                    "No OpenRouter API key configured. Set the OPENROUTER_API_KEY environment " +
                        "variable then restart."
                )
            )
            trySend(ChatEvent.Completed)
            return@channelFlow
        }

        val agent = createAgent(emit = { event -> trySend(event) })
        try {
            agent.run(userMessageWithDateContext(userMessage), sessionId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            trySend(ChatEvent.Error(failure.message ?: failure::class.simpleName ?: "Unknown error"))
        } finally {
            trySend(ChatEvent.Completed)
        }
    }

    private fun createAgent(emit: (ChatEvent) -> Unit): GraphAIAgent<String, String> =
        AIAgent(
            promptExecutor = executor,
            llmModel = model,
            toolRegistry = ToolRegistry { tools(CalendarToolSet(eventRepository, confirmationHandler)) },
            systemPrompt = SystemPrompt,
            installFeatures = {
                install(ChatMemory.Feature) {
                    chatHistoryProvider = chatHistory
                    windowSize(20)
                }
                handleEvents {
                    onLLMCallCompleted { context ->
                        context.response?.textContent()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { emit(ChatEvent.TextChunk(it)) }
                    }
                    onToolCallStarting { context ->
                        emit(ChatEvent.ToolCallStarted(context.toolName, context.toolArgs.toString()))
                    }
                    onToolCallCompleted { context ->
                        emit(ChatEvent.ToolCallCompleted(context.toolName, context.toolResult?.toString().orEmpty()))
                    }
                }
            },
        )

    /**
     * The stored chat history replaces the system prompt on later turns (the memory
     * feature replays the whole prompt), so the current date travels with each message
     * to keep relative dates ("tomorrow") correct.
     */
    private fun userMessageWithDateContext(userMessage: String): String {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        return "$userMessage\n\n(Context: the current date is $today.)"
    }

    private companion object {
        val SystemPrompt = """
            You are a concise, friendly calendar assistant inside a desktop calendar app.

            Rules:
            - Use getEvents whenever the user asks about their schedule or wants to find an event.
            - Never invent events; only report what the tools return.
            - For add/update/delete just call the tool - the app asks the user to confirm
              before anything is saved. If the user declines, do not retry unless they ask again.
            - Use YYYY-MM-DD dates and 24-hour HH:MM times in tool calls.
            - Each user message carries the current date; resolve relative dates against it.
            - Keep replies to one or two sentences unless the user asks for detail.
        """.trimIndent()
    }
}
