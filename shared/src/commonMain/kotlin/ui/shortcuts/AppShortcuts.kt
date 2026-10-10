package ui.shortcuts

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/** Actions that can be triggered from the keyboard while no dialog is open. */
enum class AppShortcut {
    /** Move the selected date one day back (Left arrow). */
    PreviousDay,

    /** Move the selected date one day forward (Right arrow). */
    NextDay,

    /** Open the add-event dialog (Enter). */
    AddEvent,
}

/**
 * Pure mapping from a key event to an [AppShortcut], or `null` when the event is not an
 * app shortcut. Modifier combinations (Ctrl/Alt/Meta) are ignored so they stay available
 * to the rest of the UI.
 */
fun shortcutForKeyEvent(event: KeyEvent): AppShortcut? {
    if (event.type != KeyEventType.KeyDown) return null
    if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return null
    return when (event.key) {
        Key.DirectionLeft -> AppShortcut.PreviousDay
        Key.DirectionRight -> AppShortcut.NextDay
        Key.Enter, Key.NumPadEnter -> AppShortcut.AddEvent
        else -> null
    }
}

/**
 * Holds the app-level shortcut callbacks and forwards key events to them.
 *
 * The keyboard logic lives here in commonMain; the desktop entry point hands the window's
 * post-content key events to [handleKeyEvent], so a shortcut only fires when no focused
 * component consumed the key first (typing in a text field, activating a focused button,
 * the chat input's Enter-to-send, ...). [App] keeps the callbacks in sync with its
 * composition state.
 *
 * While a dialog is open, [dialogsOpen] is `true` and no shortcut fires either: arrows
 * keep moving the caret in text fields, and Enter/Escape stay with the dialog even when
 * nothing inside it holds focus.
 */
class AppShortcutHandler {
    /** Set by [App] whenever a dialog (add/edit event, delete confirmation) is visible. */
    var dialogsOpen: Boolean = false

    /** Invoked with the mapped shortcut; set by [App] to mutate its hoisted state. */
    var onShortcut: ((AppShortcut) -> Unit)? = null

    /** Returns true when the event was handled and must not propagate further. */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (dialogsOpen) return false
        val shortcut = shortcutForKeyEvent(event) ?: return false
        val action = onShortcut ?: return false
        action(shortcut)
        return true
    }
}
