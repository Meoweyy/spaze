package com.sc2006.spaze.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sc2006.spaze.presentation.screens.*
import com.sc2006.spaze.presentation.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")

    object ForgotPassword : Screen("forgot_password")
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

@Composable
fun SpazeNavigation(
    navController: NavHostController = rememberNavController()
) {
    // Check authentication state
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    // ALWAYS start at login screen, then let the screens handle navigation
    // This prevents the race condition where auth state hasn't loaded yet
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            // If already authenticated, navigate to home immediately
            LaunchedEffect(authState.isAuthenticated) {
                if (authState.isAuthenticated) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
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

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onSendResetLink = { email ->
                    // You can later hook this up to Firebase or your backend
                    println("Reset link requested for: $email")
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
                onStartParkingSession = {
                    navController.navigate(Screen.ParkingTimer.route)
                }
            )
        }
    }
}