package com.sc2006.spaze.presentation.navigation

<<<<<<< Updated upstream
// Navigation.kt
// Handles app-wide navigation between Home, Search, Favorites, and Budget screens
// Person 2 - UI Layer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
=======
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
>>>>>>> Stashed changes
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sc2006.spaze.presentation.screens.*

/**
 * Navigation Routes
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Profile : Screen("profile")
    object Budget : Screen("budget")
    object ParkingTimer : Screen("parking_timer")
    object CarparkDetails : Screen("carpark_details/{carparkId}") {
        fun createRoute(carparkId: String) = "carpark_details/$carparkId"
    }
}

/**
 * Main Navigation Graph with Bottom Navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpazeNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
<<<<<<< Updated upstream
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    
    Scaffold(
        bottomBar = { 
            val mainScreens = listOf("home", "search", "favorites", "budget")
            if (currentRoute in mainScreens) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
=======
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar(currentDestination)) {
                SpazeBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { target ->
                        navController.navigate(target.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
>>>>>>> Stashed changes
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                    onNavigateToCarparkDetails = { carparkId ->
                        navController.navigate(Screen.CarparkDetails.createRoute(carparkId))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCarparkDetails = { carparkId ->
                        navController.navigate(Screen.CarparkDetails.createRoute(carparkId))
                    }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCarparkDetails = { carparkId ->
                        navController.navigate(Screen.CarparkDetails.createRoute(carparkId))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Budget.route) {
                BudgetScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ParkingTimer.route) {
                ParkingTimerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CarparkDetails.route) { backStackEntry ->
                val carparkId = backStackEntry.arguments?.getString("carparkId") ?: ""
                CarparkDetailsScreen(
                    carparkId = carparkId,
                    onNavigateBack = { navController.popBackStack() },
                    onStartParking = { navController.navigate(Screen.ParkingTimer.route) }
                )
            }
        }
    }
}

private data class BottomNavItem(
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Default.Home, "Home"),
    BottomNavItem(Screen.Search, Icons.Default.Search, "Search"),
    BottomNavItem(Screen.Favorites, Icons.Default.Favorite, "Favorites"),
    BottomNavItem(Screen.Budget, Icons.Default.AccountBalance, "Budget")
)

private fun shouldShowBottomBar(currentDestination: NavDestination?): Boolean {
    return currentDestination?.hierarchy?.any { destination ->
        bottomNavItems.any { it.screen.route == destination.route }
    } == true
}

@Composable
private fun SpazeBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.screen.route
            } == true

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.screen) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
}