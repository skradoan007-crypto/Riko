package com.rikodev.ibd3d.android.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.rikodev.ibd3d.android.R
import com.rikodev.ibd3d.android.ui.components.CheatCard
import com.rikodev.ibd3d.core.data.*
import com.rikodev.ibd3d.core.designsystem.PanelColors
import com.rikodev.ibd3d.core.model.Cheat

@Composable
fun HomeScreen(
    data: CatalogData?,
    prefs: UserPreferences,
    onCategory: (String) -> Unit,
    onPlugins: () -> Unit,
    onFavorites: () -> Unit,
    onRecents: () -> Unit,
    onCopy: (Cheat) -> Boolean,
    onFavorite: (String) -> Unit,
    onOpen: (Cheat) -> Unit,
    error: String?,
    onRetry: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.brand_icon),
                    null,
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
                )
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("IBD3D Panel", fontWeight = FontWeight.Bold, fontSize = 23.sp)
                    Text(
                        "Your offline code companion",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onFavorites) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        "Favorites",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().heightIn(min = 205.dp).clip(RoundedCornerShape(24.dp))) {
                Image(
                    painterResource(R.drawable.hero),
                    null,
                    Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier.matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PanelColors.HeroScrim, Color.Transparent)
                            )
                        )
                )
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(
                        "Indian Bikes\nDriving 3D",
                        fontSize = 26.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        "Find it. Copy it. Play.",
                        color = PanelColors.OnHero,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                    )
                    Button(onClick = { onCategory("All") }) { Text("Cheat Codes") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTile(
                    "Cheat Codes",
                    "All ${data?.items?.size?:212} codes",
                    Icons.Outlined.SportsEsports,
                    PanelColors.Codes,
                    Modifier.weight(1f),
                ) {
                    onCategory("All")
                }
                QuickTile(
                    "Vehicles",
                    "Cars, bikes & more",
                    Icons.Outlined.DirectionsCar,
                    PanelColors.Vehicles,
                    Modifier.weight(1f),
                ) {
                    onCategory("Vehicles")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTile(
                    "Animals",
                    "Creatures & characters",
                    Icons.Outlined.Pets,
                    PanelColors.Animals,
                    Modifier.weight(1f),
                ) {
                    onCategory("Animal")
                }
                QuickTile(
                    "Others",
                    "Tools & special",
                    Icons.Outlined.ViewInAr,
                    PanelColors.Others,
                    Modifier.weight(1f),
                ) {
                    onCategory("Other")
                }
            }
        }
        item {
            Card(
                onClick = onPlugins,
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Extension,
                        null,
                        Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Plugins", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("More possibilities ahead", fontSize = 12.sp)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(30.dp),
                    ) {
                        Text(
                            "Coming Soon",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
        if (error != null) item { EmptyState("Catalog unavailable", error, "Retry", onRetry) }
        val recent = prefs.recent.take(3).mapNotNull { r -> data?.items?.find { it.id == r.id } }
        if (recent.isNotEmpty()) {
            item { SectionTitle("Recently copied", onRecents) }
            items(recent, key = { "recent_${it.id}" }) { item ->
                CheatCard(
                    item,
                    item.id in prefs.favorites,
                    { onFavorite(item.id) },
                    { onCopy(item) },
                    { onOpen(item) },
                )
            }
        }
        val favorites = data?.items?.filter { it.id in prefs.favorites }?.take(3).orEmpty()
        item { SectionTitle("Your favorites", onFavorites) }
        if (favorites.isEmpty())
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        "Keep your go-to codes one tap away.",
                        Modifier.fillMaxWidth().padding(20.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        else
            items(favorites, key = { "favorite_${it.id}" }) { item ->
                CheatCard(item, true, { onFavorite(item.id) }, { onCopy(item) }, { onOpen(item) })
            }
        item {
            Text(
                "Offline first  •  Made by RikoDev",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = color),
        border = BorderStroke(1.dp, Color.White),
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 19.sp)
        TextButton(onClick = onClick) { Text("See all") }
    }
}
