package `in`.heyvinay.swipepix.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import `in`.heyvinay.swipepix.ui.theme.SwipePixMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme

/**
 * Destinations for the bottom navigation pill.
 */
enum class BottomNavTab {
    PHOTOS,
    ALBUMS,
}

/**
 * Shared floating bottom navigation bar for SwipePix containing:
 * - Left: Liquid-glass Photos/Albums pill with a smoothly SLIDING active indicator (200–300ms transition)
 * - Right: Circular glowing Clean Up action button (icon-only with wand/broom)
 *
 * Fully styled for both Dark and Light themes.
 */
@Composable
fun SwipePixBottomBar(
    currentTab: BottomNavTab,
    onPhotosClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onCleanUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current
    val pillBg = if (isDark) Color(0xE6141824) else Color(0xF5FFFFFF)
    val pillBorderBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0x4DFFFFFF), Color(0x15FFFFFF)))
    } else {
        Brush.verticalGradient(listOf(Color(0x2E000000), Color(0x12000000)))
    }
    val inactiveTint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left Floating Navigation Pill with Sliding Active Indicator
        BoxWithConstraints(
            modifier = Modifier
                .shadow(
                    elevation = if (isDark) 8.dp else 12.dp,
                    shape = CircleShape,
                    ambientColor = if (isDark) Color.Black else Color(0x33000000),
                    spotColor = if (isDark) Color(0x401E60FF) else Color(0x26000000),
                )
                .clip(CircleShape)
                .background(pillBg)
                .border(
                    width = 1.dp,
                    brush = pillBorderBrush,
                    shape = CircleShape,
                )
                .padding(4.dp),
        ) {
            // Pill dimensions: 2 equal-width tabs (approx 106dp each = 212dp total)
            val tabWidth = 106.dp
            val targetOffset = if (currentTab == BottomNavTab.PHOTOS) 0.dp else tabWidth

            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = SwipePixMotion.SPRING_SLIDE_PILL,
                label = "pillIndicatorOffset",
            )

            // Sliding Active Background Pill
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(tabWidth)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E60FF), Color(0xFF651FFF))
                        )
                    )
                    .border(1.dp, Color(0x40FFFFFF), CircleShape),
            )

            // Clickable Tab Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(tabWidth * 2),
            ) {
                // "Photos" Tab
                val photosColor by animateColorAsState(
                    targetValue = if (currentTab == BottomNavTab.PHOTOS) Color.White else inactiveTint,
                    animationSpec = tween(SwipePixMotion.DURATION_STANDARD),
                    label = "photosColor",
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(tabWidth)
                        .height(38.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPhotosClick,
                        )
                        .semantics {
                            role = Role.Tab
                            contentDescription = "Photos tab, ${if (currentTab == BottomNavTab.PHOTOS) "selected" else "not selected"}"
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = photosColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Photos",
                        color = photosColor,
                        fontSize = 14.sp,
                        fontWeight = if (currentTab == BottomNavTab.PHOTOS) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }

                // "Albums" Tab
                val albumsColor by animateColorAsState(
                    targetValue = if (currentTab == BottomNavTab.ALBUMS) Color.White else inactiveTint,
                    animationSpec = tween(SwipePixMotion.DURATION_STANDARD),
                    label = "albumsColor",
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(tabWidth)
                        .height(38.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAlbumsClick,
                        )
                        .semantics {
                            role = Role.Tab
                            contentDescription = "Albums tab, ${if (currentTab == BottomNavTab.ALBUMS) "selected" else "not selected"}"
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = albumsColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Albums",
                        color = albumsColor,
                        fontSize = 14.sp,
                        fontWeight = if (currentTab == BottomNavTab.ALBUMS) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        // Right Circular Clean Up Floating Action Button (Icon-Only, Wand/Brush) with Tactile Feedback
        val cleanUpInteraction = remember { MutableInteractionSource() }
        val isCleanUpPressed by cleanUpInteraction.collectIsPressedAsState()
        val cleanUpScale by animateFloatAsState(
            targetValue = if (isCleanUpPressed) 0.92f else 1f,
            animationSpec = SwipePixMotion.SPRING_TACTILE,
            label = "cleanUpPressScale",
        )

        Box(
            modifier = Modifier
                .size(54.dp)
                .scale(cleanUpScale)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFF2962FF),
                    spotColor = Color(0xFF651FFF),
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2962FF), Color(0xFF7C4DFF))
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0x8093C5FD), Color(0x3393C5FD))
                    ),
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = cleanUpInteraction,
                    indication = null,
                    onClick = onCleanUpClick,
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Clean Up"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
