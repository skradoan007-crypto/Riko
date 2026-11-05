package com.rikodev.ibd3d.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*

const val RESOURCE_NOTICE =
    "IBD3D Panel is an independent companion made by RikoDev, not an official Rohit Gaming Studio app. Some code information, images and model files were collected from third-party websites and community sources, including bikedriving3dcheats.com. They are not all original RikoDev creations. Rights remain with their respective owners. Source credit does not replace a license or permission. This app does not grant ownership or redistribution rights in those materials."

@Composable
fun FirstUseNotice(onClose: () -> Unit) {
    var seconds by rememberSaveable { mutableIntStateOf(7) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (seconds > 0) {
                delay(1000)
                seconds--
            }
        }
    }
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("About these resources") },
        text = {
            Column(
                Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(RESOURCE_NOTICE)
                Text(
                    "Resources may vary by game version. See About → Policies & credits for sources, privacy and rights requests."
                )
            }
        },
        confirmButton = {
            if (seconds == 0)
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, "Close resource notice")
                }
            else Text("Close available in ${seconds}s", Modifier.padding(16.dp))
        },
    )
}

@Composable
fun PoliciesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        text =
            withContext(Dispatchers.IO) {
                context.assets.open("policies.txt").bufferedReader().use { it.readText() }
            }
    }
    Column(Modifier.fillMaxSize()) {
        PageTitle("Policies & credits", onBack)
        Text(text, Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp))
    }
}
