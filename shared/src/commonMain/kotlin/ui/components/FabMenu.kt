package ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A single collapsible action that [FabMenu] reveals next to its FAB. */
data class FabAction(
    val id: String,
    val icon: ImageVector,
    val contentDescription: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

private val ActionButtonSize = 48.dp
private val ActionGap = 12.dp
private const val FabHoverScale = 1.08f

/**
 * FAB shell whose actions fan out from behind the FAB while the group is hovered or
 * keyboard focused.
 *
 * There is no expanded toggle: hover (anywhere on the group - the FAB, a revealed
 * action, or the gaps between them) *is* the reveal state. Keyboard focus joins in so
 * the actions are reachable without a pointer; when focus leaves the group the actions
 * collapse again.
 *
 * Tab order is FAB first, then the actions from innermost to outermost (list order).
 * This Compose version has no traversal-index API, so the fallback applies: children
 * are composed in tab order but placed right to left.
 *
 * @param actions actions revealed to the left of the [fab] slot, innermost first.
 * @param fab the primary floating action button.
 */
@Composable
fun FabMenu(
    actions: List<FabAction>,
    modifier: Modifier = Modifier,
    fab: @Composable () -> Unit,
) {
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()

    // Focus is tracked per element so a hand-off inside the group (FAB -> action) never
    // reads as "focus left the group" for a frame.
    var fabFocused by remember { mutableStateOf(false) }
    val actionFocus = remember { mutableStateMapOf<String, Boolean>() }
    val focusableActionIds = actions.filter { it.enabled }.map { it.id }
    val actionFocused = focusableActionIds.any { actionFocus[it] == true }

    // Drop focus bookkeeping for actions that are gone or disabled, so a re-enabled
    // action never inherits a stale "focused" flag.
    LaunchedEffect(focusableActionIds) {
        actionFocus.keys.retainAll(focusableActionIds.toSet())
    }

    val revealed = hovered || fabFocused || actionFocused

    val fabScale by animateFloatAsState(
        targetValue = if (hovered) FabHoverScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "FabMenuFabScale",
    )

    Box(modifier = modifier.hoverable(hoverSource)) {
        Layout(
            content = {
                // Composed first: the FAB is the first tab stop of the group.
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                        }
                        .onFocusChanged { fabFocused = it.hasFocus },
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Add an event") } },
                        state = rememberTooltipState(),
                    ) {
                        fab()
                    }
                }

                // Composed in list order: actions[0] is the second tab stop, right after
                // the FAB, even though it renders closest to the FAB.
                actions.forEach { action ->
                    FabActionSlot(
                        action = action,
                        revealed = revealed,
                        onFocusChanged = { focused -> actionFocus[action.id] = focused },
                    )
                }
            },
        ) { measurables, constraints ->
            // Place right to left: FAB at the trailing edge, then actions outwards.
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            val layoutWidth = placeables.sumOf { it.width }
            val layoutHeight = placeables.maxOfOrNull { it.height } ?: 0
            layout(layoutWidth, layoutHeight) {
                var x = layoutWidth
                placeables.forEach { placeable ->
                    x -= placeable.width
                    placeable.placeRelative(x, (layoutHeight - placeable.height) / 2)
                }
            }
        }
    }
}

/**
 * One animated action slot. The slot width animates from zero to button + gap, and the
 * right-aligned, clipped content emerges from behind the FAB as the width grows. Keeping
 * the gap inside the animated slot means it grows with the button instead of snapping open.
 */
@Composable
private fun FabActionSlot(
    action: FabAction,
    revealed: Boolean,
    onFocusChanged: (Boolean) -> Unit,
) {
    val revealProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "FabMenuActionReveal:${action.id}",
    )

    val slotWidth = (ActionButtonSize + ActionGap) * revealProgress.coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .width(slotWidth)
            .clipToBounds()
            .alpha(revealProgress.coerceIn(0f, 1f)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FabActionButton(action = action, onFocusChanged = onFocusChanged)
            Spacer(Modifier.width(ActionGap))
        }
    }
}

@Composable
private fun FabActionButton(
    action: FabAction,
    onFocusChanged: (Boolean) -> Unit,
) {
    // Disabled actions are muted and render as a non-clickable surface, so no click or
    // ripple can fire.
    val containerColor = if (action.enabled) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    val contentColor = if (action.enabled) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(action.contentDescription) } },
        state = rememberTooltipState(),
    ) {
        Surface(
            onClick = action.onClick,
            enabled = action.enabled,
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .size(ActionButtonSize)
                .semantics { if (!action.enabled) disabled() }
                .onFocusChanged { onFocusChanged(it.isFocused) },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
