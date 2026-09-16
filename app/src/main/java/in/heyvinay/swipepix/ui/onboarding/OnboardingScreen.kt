package `in`.heyvinay.swipepix.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import `in`.heyvinay.swipepix.R
import `in`.heyvinay.swipepix.data.permissions.PermissionState
import `in`.heyvinay.swipepix.ui.components.SwipePixAppIcon
import `in`.heyvinay.swipepix.ui.permissions.PermissionViewModel
import `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme
import `in`.heyvinay.swipepix.ui.theme.SwipePixTheme

/**
 * Two-screen Onboarding experience matching the visual direction:
 * - Screen 1: Welcome & swipe illustration, privacy pledge, Get Started CTA
 * - Screen 2: Photo access benefits, Grant Permission primary CTA, Open App Settings
 *
 * Fully theme-aware for both Dark and Light themes.
 */
@Composable
fun OnboardingScreen(
    viewModel: PermissionViewModel,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissionStatus()
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshPermissionStatus()
    }

    OnboardingContent(
        permissionState = permissionState,
        onRequestPermission = {
            launcher.launch(PermissionState.getRequiredPermissions())
        },
        onPermissionGranted = onPermissionGranted,
        modifier = modifier,
    )
}

@Composable
fun OnboardingContent(
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(permissionState) {
        if (permissionState.isGrantedOrPartial) {
            onPermissionGranted()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "onboardingPager",
        ) { step ->
            when (step) {
                0 -> WelcomeScreen(
                    onGetStarted = { currentStep = 1 },
                    onSkip = { currentStep = 1 },
                )
                else -> PhotoAccessScreen(
                    onGrantAccess = onRequestPermission,
                    onBack = { currentStep = 0 },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 1 — Welcome Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
) {
    val isDark = LocalDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top row: Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // App Icon
        SwipePixAppIcon(
            size = 96.dp,
            elevation = 16.dp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Brand Title: SwipePix
        Text(
            text = buildAnnotatedString {
                append("Swipe")
                withStyle(style = SpanStyle(color = Color(0xFF1E60FF))) {
                    append("Pix")
                }
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle
        Text(
            text = "Clean your gallery,\none swipe at a time.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Restrained Swipe Visual Representation (Cards Stack + Directional badges)
        SwipeIllustration(isDark = isDark)

        Spacer(modifier = Modifier.height(28.dp))

        // "Private by design" card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0x2BFFFFFF) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x1F1E60FF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF1E60FF),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Private by design",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Your photos stay on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Button: Get Started ->
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF1E60FF).copy(alpha = 0.3f),
                    spotColor = Color(0xFF651FFF).copy(alpha = 0.2f),
                ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E60FF), Color(0xFF651FFF))
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Get Started  →",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pagination Dots [● ○]
        PaginationDots(activeIndex = 0)

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 2 — Photo Access Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhotoAccessScreen(
    onGrantAccess: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top row: Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // App Icon
        SwipePixAppIcon(
            size = 72.dp,
            elevation = 12.dp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = buildAnnotatedString {
                append("Swipe")
                withStyle(style = SpanStyle(color = Color(0xFF1E60FF))) {
                    append("Pix")
                }
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Give SwipePix access\nto your photos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "SwipePix needs photo access to show your albums and let you clean your gallery.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3 Concise Benefit Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BenefitCard(
                icon = Icons.Default.PhoneAndroid,
                iconColor = Color(0xFF1E60FF),
                iconBg = Color(0x1F1E60FF),
                title = "On-device",
                description = "All processing happens on your device.",
                isDark = isDark,
            )
            BenefitCard(
                icon = Icons.Default.CloudOff,
                iconColor = Color(0xFF7C4DFF),
                iconBg = Color(0x1F7C4DFF),
                title = "No cloud uploads",
                description = "Your photos never leave your device.",
                isDark = isDark,
            )
            BenefitCard(
                icon = Icons.Default.Lock,
                iconColor = Color(0xFF00E676),
                iconBg = Color(0x1F00E676),
                title = "You stay in control",
                description = "Nothing is moved to Trash until you choose to apply your decisions.",
                isDark = isDark,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Button: Grant Photo Access
        Button(
            onClick = onGrantAccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF1E60FF).copy(alpha = 0.3f),
                ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E60FF), Color(0xFF651FFF))
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Grant Photo Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Button: Open App Settings
        OutlinedButton(
            onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "Open App Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your photos stay on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pagination Dots [○ ●]
        PaginationDots(activeIndex = 1)

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BenefitCard(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    description: String,
    isDark: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x2BFFFFFF) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwipeIllustration(isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Trash Badge & Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x26FF1744))
                    .border(1.dp, Color(0x59FF1744), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Trash",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "← Trash",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5252),
            )
        }

        // Center: Stack of 3 Cards
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(170.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Background Left Card
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(140.dp)
                    .rotate(-10f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF334155), Color(0xFF1E293B))
                        )
                    )
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(14.dp)),
            )

            // Background Right Card
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(140.dp)
                    .rotate(10f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF475569), Color(0xFF334155))
                        )
                    )
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(14.dp)),
            )

            // Front Main Card
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .height(150.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E3A8A), Color(0xFF1E1B4B))
                        )
                    )
                    .border(1.5.dp, Color(0x8093C5FD), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                // Miniature photo preview placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Photo",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }

        // Right: Keep Badge & Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x2600E676))
                    .border(1.dp, Color(0x5900E676), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Keep",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keep →",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E676),
            )
        }
    }
}

@Composable
private fun PaginationDots(activeIndex: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(2) { index ->
            val isSelected = index == activeIndex
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (isSelected) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF1E60FF)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(name = "Onboarding Screen", showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    SwipePixTheme {
        OnboardingContent(
            permissionState = PermissionState.Denied,
            onRequestPermission = {},
            onPermissionGranted = {},
        )
    }
}

