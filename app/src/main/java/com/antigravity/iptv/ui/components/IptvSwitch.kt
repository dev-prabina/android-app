package com.antigravity.iptv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A professional, high-performance Android toggle switch built with Jetpack Compose.
 *
 * Provides:
 * - Clear ON state (vibrant primary track, crisp white thumb with checkmark)
 * - Clear OFF state (structured neutral track with outline, distinct contrasting thumb)
 * - Physics-based spring animation for thumb translation
 * - Full accessibility semantics and 48dp touch target
 * - High contrast in both dark and light modes
 * - Correct disabled state handling
 */
@Composable
fun IptvSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackWidth: Dp = 50.dp,
    trackHeight: Dp = 28.dp,
    thumbSize: Dp = 22.dp,
    thumbPadding: Dp = 3.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Spring animation for smooth, snappy thumb motion
    val maxOffset = trackWidth - thumbSize - (thumbPadding * 2)
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SwitchThumbOffset"
    )

    // Animated colors for track and thumb
    val primaryColor = MaterialTheme.colorScheme.primary
    val uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (checked) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    val animatedTrackColor by animateColorAsState(
        targetValue = if (checked) primaryColor else uncheckedTrackColor,
        label = "SwitchTrackColor"
    )

    val animatedThumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else MaterialTheme.colorScheme.outline,
        label = "SwitchThumbColor"
    )

    // Accessible touch container (minimum 48dp height/width)
    Box(
        modifier = modifier
            .semantics {
                this.role = Role.Switch
                this.stateDescription = if (checked) "On" else "Off"
            }
            .alpha(if (enabled) 1f else 0.38f)
            .size(width = 56.dp, height = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 24.dp),
                enabled = enabled && onCheckedChange != null,
                onClick = { onCheckedChange?.invoke(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Track
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(animatedTrackColor)
                .border(
                    width = 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(trackHeight / 2)
                )
                .padding(thumbPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            // Sliding Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .shadow(
                        elevation = if (checked) 3.dp else 1.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    .clip(CircleShape)
                    .background(animatedThumbColor),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
