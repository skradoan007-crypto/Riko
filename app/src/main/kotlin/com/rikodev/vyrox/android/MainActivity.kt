package com.rikodev.vyrox.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rikodev.vyrox.core.design.VyRoxTheme
import com.rikodev.vyrox.feature.editor.EditorScreen
import com.rikodev.vyrox.feature.export.ExportScreen
import com.rikodev.vyrox.feature.home.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable { mutableStateOf(systemDark) }
            val navController = rememberNavController()

            VyRoxTheme(darkTheme = isDarkTheme) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = !isDarkTheme },
                            onOpenEditor = { projectId ->
                                navController.navigate("editor/$projectId")
                            }
                        )
                    }
                    composable(
                        route = "editor/{projectId}",
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                    ) {
                        EditorScreen(
                            onBack = { navController.popBackStack() },
                            onExportClick = { projectId ->
                                navController.navigate("export/$projectId")
                            }
                        )
                    }
                    composable(
                        route = "export/{projectId}",
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                    ) {
                        ExportScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
