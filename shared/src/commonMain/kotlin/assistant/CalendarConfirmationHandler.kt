package assistant

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A write action the assistant is waiting for the user to approve or deny. */
data class PendingConfirmation(
    val toolName: String,
    val description: String,
)

/**
 * Two-sided bridge between the agent and the UI for write actions:
 *
 * - The agent's write tools call [awaitDecision] before mutating anything and suspend
 *   until the user decides.
 * - The UI observes [pending] and resolves the request with [approve] or [deny].
 *
 * While a decision is pending the agent is parked inside the tool call; the tool returns
 * a refusal message to the model when the user denies, so the model can respond accordingly.
 */
class CalendarConfirmationHandler {

    private val _pending = MutableStateFlow<PendingConfirmation?>(null)

    /** The request waiting for a decision, or null when nothing is pending. */
    val pending: StateFlow<PendingConfirmation?> = _pending.asStateFlow()

    private var decision: CompletableDeferred<Boolean>? = null

    /** Suspends until the user approves (true) or denies (false) the described action. */
    suspend fun awaitDecision(toolName: String, description: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        decision = deferred
        _pending.value = PendingConfirmation(toolName = toolName, description = description)
        return try {
            deferred.await()
        } finally {
            _pending.value = null
            decision = null
        }
    }

    /** Approves the pending action. No-op when nothing is pending. */
    fun approve() {
        decision?.complete(true)
    }

    /** Denies the pending action. No-op when nothing is pending. */
    fun deny() {
        decision?.complete(false)
    }
}
