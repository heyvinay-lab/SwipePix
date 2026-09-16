package `in`.heyvinay.swipepix.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.heyvinay.swipepix.R

/**
 * Official branded SwipePix app icon component.
 *
 * Displays the high-fidelity 3D SwipePix artwork: curved deep-blue/purple gradient background,
 * layered fanning photo cards, glowing sun, and orbiting cyan swoosh ring,
 * housed in an elevated squircle with colored ambient glow and subtle glass border highlight.
 */
@Composable
fun SwipePixAppIcon(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    elevation: Dp = 16.dp,
) {
    val cornerRadius = size * 0.22f
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0xFF1E60FF).copy(alpha = 0.45f),
                spotColor = Color(0xFF651FFF).copy(alpha = 0.35f),
            )
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.10f),
                    ),
                ),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_swipepix_logo),
            contentDescription = "SwipePix Logo",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
