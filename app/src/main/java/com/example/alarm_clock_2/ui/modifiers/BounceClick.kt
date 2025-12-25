package com.example.alarm_clock_2.ui.modifiers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

enum class ButtonState { Pressed, Idle }

fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit = {}
) = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val scale by animateFloatAsState(
        if (buttonState == ButtonState.Pressed) scaleDown else 1f,
        label = "bounce_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // Disable default ripple if using bounce, or keep it? Let's disable for pure bounce or maybe keep it.
                               // Usually bounce replaces ripple or works with it. Let's keep ripple separate via standard clickable if needed, 
                               // but here we are wrapping clickable behavior.
                               // actually, usually we want both. But to keep it simple and non-interfering with existing clickable, 
                               // maybe I should just make a modifier that handles the animation state and let the user add .clickable?
                               // No, it's easier to bundle it if I'm replacing clickable. 
                               // However, existing components already have clickable.
                               // Let's make a version that ONLY handles the animation based on press state, without handling the click event itself?
                               // That's harder with standard clickable.
                               // Let's stick to the "bounceClick" that handles the click.
            onClick = onClick
        )
        .pointerInput(buttonState) {
            awaitPointerEventScope {
                buttonState = if (buttonState == ButtonState.Pressed) {
                    waitForUpOrCancellation()
                    ButtonState.Idle
                } else {
                    awaitFirstDown(false)
                    ButtonState.Pressed
                }
            }
        }
}

// A modifier that only adds the bounce effect on press, without handling the click event itself.
// Useful for components that already have a clickable surface (like Cards or Buttons).
fun Modifier.pressClickEffect(
    scaleDown: Float = 0.95f
) = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val scale by animateFloatAsState(
        if (buttonState == ButtonState.Pressed) scaleDown else 1f,
        label = "press_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(false)
                    buttonState = ButtonState.Pressed
                    waitForUpOrCancellation()
                    buttonState = ButtonState.Idle
                }
            }
        }
}

