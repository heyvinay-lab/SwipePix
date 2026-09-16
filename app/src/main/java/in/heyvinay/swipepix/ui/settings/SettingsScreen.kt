package `in`.heyvinay.swipepix.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import `in`.heyvinay.swipepix.BuildConfig
import `in`.heyvinay.swipepix.data.config.SwipePixWebsiteConfig
import `in`.heyvinay.swipepix.ui.util.WebIntentUtils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.heyvinay.swipepix.data.permissions.PermissionState
import `in`.heyvinay.swipepix.data.preferences.AppTheme
import `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme
import kotlinx.coroutines.launch

/**
 * Settings screen — single scrollable page matching the approved design.
 *
 * Sections:
 * - Appearance (Theme selection bottom sheet)
 * - Cleaning (Remember progress, Show undo option toggles)
 * - Trash (Move to Android Trash, Confirm before applying toggle, View Trash action)
 * - Permissions (Photo access status, Manage in system settings)
 * - Privacy & About (Privacy commitment, Open source licenses, App version)
 *
 * Fully theme-aware for both Dark and Light themes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onViewTrashClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val rememberProgress by viewModel.rememberProgress.collectAsStateWithLifecycle()
    val showUndoOption by viewModel.showUndoOption.collectAsStateWithLifecycle()
    val confirmBeforeApplying by viewModel.confirmBeforeApplying.collectAsStateWithLifecycle()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = LocalDarkTheme.current
    val scope = rememberCoroutineScope()

    var showThemeSheet by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showTrashInfoDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Customize your experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── APPEARANCE ──
            SettingsSectionHeader(title = "APPEARANCE")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = "Choose how the app looks",
                    onClick = { showThemeSheet = true },
                    trailing = {
                        val label = when (currentTheme) {
                            AppTheme.SYSTEM -> "System Default"
                            AppTheme.LIGHT -> "Light Theme"
                            AppTheme.DARK -> "Dark Theme"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0x26FFFFFF) else Color(0xFFF1F5F9)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                                    CircleShape,
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── CLEANING ──
            SettingsSectionHeader(title = "CLEANING")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.History,
                    title = "Remember progress",
                    subtitle = "Save your cleaning progress and resume later",
                    trailing = {
                        Switch(
                            checked = rememberProgress,
                            onCheckedChange = { viewModel.setRememberProgress(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    title = "Show undo option",
                    subtitle = "Allow undo after swiping a photo",
                    trailing = {
                        Switch(
                            checked = showUndoOption,
                            onCheckedChange = { viewModel.setShowUndoOption(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── TRASH ──
            SettingsSectionHeader(title = "TRASH")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.DeleteOutline,
                    title = "Move to Android Trash",
                    subtitle = "Deleted photos are moved to your device's Trash",
                    onClick = { showTrashInfoDialog = true },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Shield,
                    title = "Confirm before applying",
                    subtitle = "Show a confirmation before moving to Trash",
                    trailing = {
                        Switch(
                            checked = confirmBeforeApplying,
                            onCheckedChange = { viewModel.setConfirmBeforeApplying(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.FolderOpen,
                    title = "View Trash",
                    subtitle = "Open your device's Trash folder",
                    onClick = onViewTrashClick,
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── PERMISSIONS ──
            SettingsSectionHeader(title = "PERMISSIONS")
            SettingsCard {
                val (statusText, statusColor) = when (permissionState) {
                    is PermissionState.Granted -> "Full access granted" to Color(0xFF00E676)
                    is PermissionState.Partial -> "Limited access" to Color(0xFFFFB300)
                    else -> "Permission required" to Color(0xFFFF5252)
                }

                SettingsRow(
                    icon = Icons.Default.Security,
                    title = "Photo access",
                    subtitle = statusText,
                    subtitleColor = statusColor,
                    onClick = { viewModel.openAppSettings(context) },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Settings,
                    title = "Manage in system settings",
                    subtitle = "Change photo access from Android settings",
                    onClick = { viewModel.openAppSettings(context) },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SUPPORT ──
            SettingsSectionHeader(title = "SUPPORT")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Refresh,
                    title = "Check for Updates",
                    subtitle = "Check the latest SwipePix version and changelog",
                    onClick = { WebIntentUtils.openUrl(context, SwipePixWebsiteConfig.UPDATES_URL) },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open updates in browser",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Favorite,
                    title = "Donate",
                    subtitle = "Support the development of SwipePix",
                    onClick = { WebIntentUtils.openUrl(context, SwipePixWebsiteConfig.DONATE_URL) },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open donation page in browser",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Email,
                    title = "Send Feedback",
                    subtitle = "Share feedback or report a problem",
                    onClick = { WebIntentUtils.openUrl(context, SwipePixWebsiteConfig.FEEDBACK_URL) },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open feedback page in browser",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── PRIVACY & ABOUT ──
            SettingsSectionHeader(title = "PRIVACY & ABOUT")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "Privacy",
                    subtitle = "100% on-device. No cloud uploads, no accounts, no tracking.",
                    onClick = { showPrivacyDialog = true },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Description,
                    title = "Open source licenses",
                    subtitle = "Third-party licenses and attributions",
                    onClick = { showLicensesDialog = true },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingsDivider()
                val appVersion = remember {
                    try {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        pInfo.versionName ?: BuildConfig.VERSION_NAME
                    } catch (e: Exception) {
                        BuildConfig.VERSION_NAME
                    }
                }
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "App version",
                    subtitle = "Version $appVersion",
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                )
                Text(
                    text = "Built with care for a cleaner gallery",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Theme Selection Bottom Sheet ──
    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Text(
                    text = "Choose Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))

                ThemeOptionRow(
                    title = "System Default",
                    description = "Follow your device's system theme",
                    isSelected = currentTheme == AppTheme.SYSTEM,
                    onClick = {
                        viewModel.updateTheme(AppTheme.SYSTEM)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showThemeSheet = false }
                    },
                )
                ThemeOptionRow(
                    title = "Light Theme",
                    description = "Clean, light aesthetic",
                    isSelected = currentTheme == AppTheme.LIGHT,
                    onClick = {
                        viewModel.updateTheme(AppTheme.LIGHT)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showThemeSheet = false }
                    },
                )
                ThemeOptionRow(
                    title = "Dark Theme",
                    description = "Deep navy and black dark palette",
                    isSelected = currentTheme == AppTheme.DARK,
                    onClick = {
                        viewModel.updateTheme(AppTheme.DARK)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showThemeSheet = false }
                    },
                )
            }
        }
    }

    // ── Privacy Dialog ──
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy by Design", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "SwipePix never uploads your photos to any remote server or cloud. " +
                    "All photo loading, decision tracking, and cleaning are executed 100% locally on your device."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            },
        )
    }

    // ── Trash Info Dialog ──
    if (showTrashInfoDialog) {
        AlertDialog(
            onDismissRequest = { showTrashInfoDialog = false },
            title = { Text("Move to Android Trash", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "When you complete a cleaning session and tap 'Move to Trash', SwipePix uses Android's native MediaStore Trash API. " +
                    "Photos remain in your system Trash for 30 days before Android permanently removes them."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTrashInfoDialog = false }) {
                    Text("OK")
                }
            },
        )
    }

    // ── Licenses Dialog ──
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "SwipePix is built using Jetpack Compose, Material 3, AndroidX Room, Coil, Kotlin Coroutines, and Hilt. " +
                    "Licensed under Apache 2.0."
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val isDark = LocalDarkTheme.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline,
                shape = shape,
            ),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable () -> Unit,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun SettingsDivider() {
    val isDark = LocalDarkTheme.current
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = if (isDark) Color(0x1FFFFFFF) else Color(0x12000000),
    )
}

@Composable
private fun ThemeOptionRow(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
