package `in`.heyvinay.swipepix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme

/**
 * Shared, unified top header component for both Photos and Albums gallery destinations.
 *
 * Guarantees pixel-identical:
 * - Title typography (32sp bold, -0.5sp letter spacing)
 * - Subtitle typography (14sp normal, onSurfaceVariant)
 * - Settings action button (42dp circle with glass border)
 * - Padding and vertical alignment
 * - Dual-theme styling
 *
 * An optional [chipsContent] slot can be provided for horizontal filter chips (e.g. on the Photos tab).
 */
@Composable
fun GalleryHeader(
    title: String,
    subtitle: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    chipsContent: (@Composable () -> Unit)? = null,
) {
    val isDark = LocalDarkTheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
    ) {
        // Top Title & Settings Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Subtle Glass Settings Button (identical across Photos and Albums)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x24FFFFFF) else Color(0xFFF1F5F9))
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                        shape = CircleShape,
                    )
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Optional filter chips row (e.g. on Photos)
        if (chipsContent != null) {
            Spacer(modifier = Modifier.height(14.dp))
            chipsContent()
        }
    }
}
