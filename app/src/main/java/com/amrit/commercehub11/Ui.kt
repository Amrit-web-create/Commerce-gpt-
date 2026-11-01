package com.amrit.commercehub11

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** ripple-btn: dabane par halka sa chhota hota hai (scale .96) */
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, label = "tap")
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/** bina animation ke simple click (indication nahi) */
fun Modifier.plainClick(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    this.clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
fun GradientLine(color: Color, width: Dp = 1.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = width.toPx(), cap = StrokeCap.Butt)
    }
}
