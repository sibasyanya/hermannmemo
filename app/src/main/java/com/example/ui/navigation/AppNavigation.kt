package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DeckDetailScreen
import com.example.ui.screens.DecksScreen
import com.example.ui.screens.InstructionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.StudySessionScreen
import com.example.ui.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Decks : Screen("decks", "Темы", Icons.Default.School)
    object Stats : Screen("stats", "Память", Icons.Default.BarChart)
    object Instructions : Screen("instructions", "Инструкции", Icons.AutoMirrored.Filled.MenuBook)
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
    object StudySession : Screen("study_session", "Сессия")
    object DeckDetail : Screen("deck_detail/{deckId}", "Детали") {
        fun createRoute(deckId: Long) = "deck_detail/$deckId"
    }
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Decks,
        Screen.Stats,
        Screen.Instructions,
        Screen.Settings
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Decks.route,
        Screen.Stats.route,
        Screen.Instructions.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(imageVector = it, contentDescription = screen.title) } },
                            label = { Text(screen.title) },
                            selected = selected,
                            modifier = Modifier.testTag("nav_${screen.route}"),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Decks.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Decks.route) {
                DecksScreen(
                    viewModel = viewModel,
                    onStartStudy = { deckId, title ->
                        viewModel.startStudySession(deckId, title)
                        navController.navigate(Screen.StudySession.route)
                    },
                    onOpenDeckDetail = { deckId ->
                        navController.navigate(Screen.DeckDetail.createRoute(deckId))
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatisticsScreen(viewModel = viewModel)
            }

            composable(Screen.Instructions.route) {
                InstructionsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }

            composable(Screen.StudySession.route) {
                StudySessionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.DeckDetail.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getLong("deckId") ?: 0L
                DeckDetailScreen(
                    deckId = deckId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onStartStudy = { dId, title ->
                        viewModel.startStudySession(dId, title)
                        navController.navigate(Screen.StudySession.route)
                    }
                )
            }
        }
    }
}
