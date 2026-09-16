package `in`.heyvinay.swipepix.ui.cleanup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.heyvinay.swipepix.ui.theme.SwipeKeepGreen
import `in`.heyvinay.swipepix.ui.theme.SwipeTrashRed
import kotlin.math.abs

@Composable
fun SwipeFeedbackOverlay(
    dragProgress: Float, // -1.0 (left/trash) to +1.0 (right/keep)
    modifier: Modifier = Modifier,
) {
    val isLeft = dragProgress < -0.05f
    val isRight = dragProgress > 0.05f
    val intensity = abs(dragProgress).coerceIn(0f, 1f)

    if (!isLeft && !isRight) return

    val accentColor = if (isLeft) SwipeTrashRed else SwipeKeepGreen
    val glowColor = accentColor.copy(alpha = intensity * 0.85f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = (3.dp * intensity).coerceAtLeast(1.5.dp),
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, accentColor.copy(alpha = intensity * 0.4f)),
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .background(accentColor.copy(alpha = intensity * 0.12f))
    ) {
        // Floating decision card
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(0.85f + 0.15f * intensity)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isLeft) Color(0xCCB71C1C) else Color(0xCC1B5E20)
                )
                .border(
                    width = 1.5.dp,
                    color = accentColor.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isLeft) Icons.Default.Delete else Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (isLeft) "Trash" else "Keep",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = if (isLeft) "Release to move" else "Release to keep",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
