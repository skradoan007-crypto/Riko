package com.rikodev.ibd3d.android.ui.components

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.rikodev.ibd3d.core.model.Cheat
import kotlinx.coroutines.delay

@Composable
fun CheatImage(item: Cheat, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Image,
            null,
            Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = .45f),
        )
        item.image
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->
                AsyncImage(
                    model =
                        Uri.Builder()
                            .scheme("file")
                            .appendPath("android_asset")
                            .apply { path.split('/').forEach { appendPath(it) } }
                            .build()
                            .toString(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
    }
}

@Composable
fun CheatCard(
    item: Cheat,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onCopy: () -> Boolean,
    onOpen: () -> Unit,
) {
    var copied by remember(item.id) { mutableStateOf(false) }
    var copySequence by remember(item.id) { mutableIntStateOf(0) }
    LaunchedEffect(copySequence) {
        if (copied) {
            delay(1300)
            copied = false
        }
    }
    OutlinedCard(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheatImage(item, Modifier.size(64.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    item.code,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
                Text(
                    item.badge,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                IconButton(
                    onClick = {
                        copied = onCopy()
                        if (copied) copySequence++
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Copy ${item.name} code ${item.code}"
                            if (copied) stateDescription = "Copied"
                        },
                ) {
                    Icon(
                        if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconToggleButton(
                    checked = favorite,
                    onCheckedChange = { onFavorite() },
                    modifier = Modifier.semantics { contentDescription = "Favorite ${item.name}" },
                ) {
                    Icon(
                        if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        null,
                        tint =
                            if (favorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
