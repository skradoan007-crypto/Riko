package com.rikodev.ibd3d.android.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.rikodev.ibd3d.android.BuildConfig
import com.rikodev.ibd3d.android.R
import com.rikodev.ibd3d.android.updates.UpdateState
import com.rikodev.ibd3d.core.data.UserPreferences

@Composable
fun PluginsScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                Icons.Outlined.Extension,
                null,
                Modifier.padding(28.dp).size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            "A little more is coming.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text("Coming Soon", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            "The Plugins section is still being developed. Future app updates may bring new features here.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Your offline cheat codes are ready to use today.", textAlign = TextAlign.Center)
    }
}

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    update: UpdateState,
    onUpdate: () -> Unit,
    onComplete: () -> Unit,
    onFeedback: (Boolean) -> Unit,
    onAccent: (String) -> Unit,
    onFavorites: () -> Unit,
    onRecents: () -> Unit,
    onAbout: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle("Settings")
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingRow(
                if (update.downloaded) "Restart to update" else "Check for Update",
                update.text,
                Icons.Outlined.SystemUpdate,
                if (update.downloaded) onComplete else onUpdate,
                !update.busy,
            )
            if (update.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Label("YOUR COLLECTION")
            SettingRow(
                "Favorites",
                "${prefs.favorites.size} saved codes",
                Icons.Outlined.FavoriteBorder,
                onFavorites,
            )
            SettingRow(
                "Recent copies",
                "Your latest copied codes",
                Icons.Outlined.History,
                onRecents,
            )
            Label("GENERAL")
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingIcon(Icons.Outlined.ContentCopy)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Copy feedback", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Show an extra “Copied” message",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = prefs.copyFeedback,
                        onCheckedChange = onFeedback,
                        modifier = Modifier.semantics { contentDescription = "Copy feedback" },
                    )
                }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Accent color", fontWeight = FontWeight.SemiBold)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("Purple", "Blue", "Rose").forEach { v ->
                            FilterChip(
                                modifier = Modifier.heightIn(min = 48.dp),
                                selected = prefs.accent == v,
                                onClick = { onAccent(v) },
                                label = { Text(v) },
                            )
                        }
                    }
                }
            }
            Label("ABOUT")
            SettingRow(
                "About IBD3D Panel",
                "v${BuildConfig.VERSION_NAME} • Offline",
                Icons.Outlined.Info,
                onAbout,
            )
            SettingRow("RikoDev", "Independent Android app developer", Icons.Outlined.Code, onAbout)
            Text(
                "Android may also show its own clipboard confirmation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Label(s: String) {
    Text(
        s,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
    )
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Icon(
            icon,
            null,
            Modifier.padding(9.dp).size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(icon)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle("About", onBack)
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painterResource(R.drawable.brand_icon),
                "IBD3D Panel icon",
                Modifier.size(112.dp).clip(RoundedCornerShape(28.dp)),
            )
            Text(
                "IBD3D Panel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("By RikoDev", color = MaterialTheme.colorScheme.primary)
            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            Text(
                BuildConfig.APPLICATION_ID,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            HorizontalDivider()
            Text("Your codes. Always with you.", style = MaterialTheme.typography.titleLarge)
            Text(
                "Browse, search and copy the bundled catalog offline. Favorites, recent copies and settings stay on this device. New content arrives through Google Play app updates."
            )
            Text(
                "Privacy",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start),
            )
            Text(
                "No accounts, ads, analytics or custom server. This app does not upload your favorites or copy history. Google Play handles update checks. Uninstalling clears local app data. Clipboard contents are managed by Android and may be accessed by apps you paste into."
            )
            Text(
                "Catalog & attribution",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start),
            )
            Text(
                "Official list, community claims, experimental combinations and RGS Tool commands are labeled separately. Source labels do not guarantee compatibility with every game version. Images are illustrative; unconfirmed matches are noted in details."
            )
            Text(
                "Unofficial companion app. Not affiliated with Rohit Gaming Studio.",
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
