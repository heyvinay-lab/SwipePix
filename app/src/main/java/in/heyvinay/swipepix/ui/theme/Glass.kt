package `in`.heyvinay.swipepix.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Styling defaults and brush factories for liquid glass components.
 */
object GlassDefaults {
    val DefaultCornerRadius: Dp = 20.dp
    val BorderWidth: Dp = 1.dp
    val PillShape: Shape = CircleShape

    @Composable
    fun surfaceBrush(isDark: Boolean = LocalDarkTheme.current): Brush {
        return if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x26FFFFFF),
                    Color(0x10FFFFFF),
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xE6FFFFFF),
                    Color(0xCCFFFFFF),
                )
            )
        }
    }

    @Composable
    fun borderBrush(isDark: Boolean = LocalDarkTheme.current): Brush {
        return if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x59FFFFFF),
                    Color(0x14FFFFFF),
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x33000000),
                    Color(0x0D000000),
                )
            )
        }
    }
}

/**
 * Modifier that applies the liquid-glass visual style:
 * translucent gradient surface, light-catching gradient border, and clipping.
 */
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(GlassDefaults.DefaultCornerRadius),
    surfaceBrush: Brush? = null,
    borderBrush: Brush? = null,
    borderWidth: Dp = GlassDefaults.BorderWidth,
    elevation: Dp = 0.dp,
): Modifier {
    val shadowMod = if (elevation > 0.dp) {
        Modifier.shadow(elevation = elevation, shape = shape, clip = false)
    } else {
        Modifier
    }

    return this
        .then(shadowMod)
        .clip(shape)
        .then(
            if (surfaceBrush != null) {
                Modifier.background(surfaceBrush)
            } else {
                Modifier.background(Color(0x1CFFFFFF))
            }
        )
        .then(
            if (borderBrush != null) {
                Modifier.border(width = borderWidth, brush = borderBrush, shape = shape)
            } else {
                Modifier.border(
                    width = borderWidth,
                    brush = Brush.verticalGradient(
                        listOf(Color(0x4DFFFFFF), Color(0x14FFFFFF))
                    ),
                    shape = shape
                )
            }
        )
}

/**
 * Reusable GlassSurface container for floating bars, dialogs, and panels.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassDefaults.DefaultCornerRadius),
    isDark: Boolean = LocalDarkTheme.current,
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.glassEffect(
            shape = shape,
            surfaceBrush = GlassDefaults.surfaceBrush(isDark),
            borderBrush = GlassDefaults.borderBrush(isDark),
            borderWidth = GlassDefaults.BorderWidth,
            elevation = elevation,
        ),
        content = content,
    )
}

/**
 * Reusable GlassCard for list items, albums, or selection options.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean = LocalDarkTheme.current,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .glassEffect(
                shape = shape,
                surfaceBrush = GlassDefaults.surfaceBrush(isDark),
                borderBrush = GlassDefaults.borderBrush(isDark),
                borderWidth = GlassDefaults.BorderWidth,
                elevation = elevation,
            )
            .then(clickableModifier),
        content = content,
    )
}

/**
 * A compact, translucent pill badge for directional indicators (KEEP / TRASH)
 * or metadata chips.
 */
@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Color.White,
    containerColor: Color = tint.copy(alpha = 0.2f),
    borderColor: Color = tint.copy(alpha = 0.45f),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}
