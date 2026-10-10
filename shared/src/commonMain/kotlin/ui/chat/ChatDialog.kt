package ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ui.formatTime

private val PopupMaxWidth = 560.dp
private val PopupMaxHeight = 520.dp
private val PopupWindowMargin = 32.dp
private const val ScrimAlpha = 0.32f
private val AutoScrollBottomThreshold = 64.dp

private val UserBubbleShape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
private val BotBubbleShape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)

/**
 * Modal AI chat popup.
 *
 * Rendered as the last child of the app's content [Box], so the scrim and the card cover
 * the calendar and the FABs and - because pointer hit testing stops at the topmost hit
 * child - they occlude everything behind them (clicks, scrolling and hover included).
 * The popup has no appear/disappear animation: it is a plain conditional.
 *
 * All conversation state is hoisted: [messages] and [awaitingReply] live in `App`, so a
 * pending reply survives closing the popup. The draft text does not - it is local state
 * and resets on every open.
 */
@Composable
fun ChatDialog(
    messages: List<ChatMessage>,
    awaitingReply: Boolean,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val inputFocusRequester = remember { FocusRequester() }
    val cardFocusRequester = remember { FocusRequester() }

    // Keep keyboard focus inside the dialog: the input normally, the card while the input
    // is disabled mid-reply (so Escape still reaches this dialog's key handler).
    LaunchedEffect(awaitingReply) {
        if (awaitingReply) cardFocusRequester.requestFocus() else inputFocusRequester.requestFocus()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
    ) {
        val cardWidth = minOf(PopupMaxWidth, (maxWidth - PopupWindowMargin).coerceAtLeast(1.dp))
        val cardHeight = minOf(PopupMaxHeight, (maxHeight - PopupWindowMargin).coerceAtLeast(1.dp))

        // Scrim: dims the app, occludes the content behind, and dismisses on click.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
        )

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(cardWidth, cardHeight)
                .focusRequester(cardFocusRequester)
                .focusable()
                // Keep a (non-consuming) pointer handler on the card so clicks on its
                // non-interactive areas count as hits on the card instead of falling
                // through to the scrim and dismissing the dialog.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }
                .semantics { paneTitle = "AI Assistant" },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            ChatDialogContent(
                messages = messages,
                awaitingReply = awaitingReply,
                bubbleMaxWidth = cardWidth * 0.7f,
                inputFocusRequester = inputFocusRequester,
                onSend = onSend,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun ChatDialogContent(
    messages: List<ChatMessage>,
    awaitingReply: Boolean,
    bubbleMaxWidth: Dp,
    inputFocusRequester: FocusRequester,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var input by remember { mutableStateOf("") }
    val canSend = input.isNotBlank() && !awaitingReply

    fun send() {
        if (!canSend) return
        onSend(input)
        input = ""
    }

    val itemCount = messages.size + if (awaitingReply) 1 else 0
    var isInitialScroll by remember { mutableStateOf(true) }

    // Auto-scroll on new items, but only when the user was already near the bottom. The
    // scroll position is read before scrolling, so a user who scrolled up is not yanked
    // down by an incoming bot message. The first pass jumps straight to the bottom.
    LaunchedEffect(messages.size, awaitingReply) {
        if (itemCount == 0) return@LaunchedEffect
        val layout = listState.layoutInfo
        val lastVisible = layout.visibleItemsInfo.lastOrNull()
        val gapToBottom = if (lastVisible == null) {
            0
        } else {
            layout.viewportEndOffset - (lastVisible.offset + lastVisible.size)
        }
        val nearBottom = lastVisible == null || gapToBottom <= with(density) { AutoScrollBottomThreshold.roundToPx() }
        if (nearBottom) {
            if (isInitialScroll) {
                listState.scrollToItem(itemCount - 1)
                isInitialScroll = false
            } else {
                listState.animateScrollToItem(itemCount - 1)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close chat")
            }
        }

        HorizontalDivider()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(message = message, maxWidth = bubbleMaxWidth)
            }
            if (awaitingReply) {
                item(key = "typing-indicator") {
                    TypingIndicator()
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(inputFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                            send()
                            true
                        } else {
                            false
                        }
                    },
                enabled = !awaitingReply,
                singleLine = true,
                placeholder = { Text("Type a message...") },
            )
            IconButton(onClick = { send() }, enabled = canSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, maxWidth: Dp) {
    val isUser = message.role == Role.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = maxWidth),
            shape = if (isUser) UserBubbleShape else BotBubbleShape,
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isUser) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatTime(
                        message.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).time
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typingIndicator")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            shape = BotBubbleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { index ->
                    val dotAlpha by transition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 450,
                                delayMillis = index * 150,
                                easing = LinearEasing,
                            ),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "typingDot$index",
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(dotAlpha)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                    )
                }
            }
        }
    }
}
