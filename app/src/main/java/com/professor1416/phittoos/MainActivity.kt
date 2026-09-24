package com.professor1416.phittoos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.professor1416.phittoos.reminder.ReminderNotificationHelper
import com.professor1416.phittoos.ui.navigation.Screen
import com.professor1416.phittoos.ui.screens.activity.ActivityScreen
import com.professor1416.phittoos.ui.screens.addtransaction.AddTransactionScreen
import com.professor1416.phittoos.ui.screens.frienddetail.FriendDetailScreen
import com.professor1416.phittoos.ui.screens.home.HomeScreen
import com.professor1416.phittoos.ui.screens.onboarding.OnboardingScreen
import com.professor1416.phittoos.ui.screens.settings.SettingsScreen
import com.professor1416.phittoos.ui.theme.PhittoosTheme
import com.professor1416.phittoos.ui.viewmodel.ActivityViewModel
import com.professor1416.phittoos.ui.viewmodel.ActivityViewModelFactory
import com.professor1416.phittoos.ui.viewmodel.AddTransactionViewModel
import com.professor1416.phittoos.ui.viewmodel.AddTransactionViewModelFactory
import com.professor1416.phittoos.ui.viewmodel.FriendDetailViewModel
import com.professor1416.phittoos.ui.viewmodel.FriendDetailViewModelFactory
import com.professor1416.phittoos.ui.viewmodel.HomeViewModel
import com.professor1416.phittoos.ui.viewmodel.HomeViewModelFactory
import com.professor1416.phittoos.ui.viewmodel.SettingsViewModel
import com.professor1416.phittoos.ui.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PhittoosApplication
        val userPreferences = app.userPreferences
        val friendIdFromNotification = intent?.getLongExtra(ReminderNotificationHelper.EXTRA_FRIEND_ID, -1L)?.takeIf { it > 0 }

        setContent {
            val themeMode by userPreferences.themeModeFlow.collectAsStateWithLifecycle()
            PhittoosTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhittoosNavApp(app, initialFriendId = friendIdFromNotification)
                }
            }
        }
    }
}

@Composable
fun PhittoosNavApp(app: PhittoosApplication, initialFriendId: Long? = null) {
    val navController = rememberNavController()
    val userPreferences = app.userPreferences

    LaunchedEffect(initialFriendId) {
        if (initialFriendId != null && initialFriendId > 0 && userPreferences.hasCompletedOnboarding) {
            val friend = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                app.repository.getFriendByIdOnce(initialFriendId)
            }
            if (friend != null) {
                navController.navigate(Screen.FriendDetail.createRoute(initialFriendId))
            } else {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        }
    }

    val startDestination = if (userPreferences.hasCompletedOnboarding) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Onboarding Screen
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                userPreferences = userPreferences,
                onCompleteOnboarding = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Home / Dashboard Screen
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(app.repository, app.userPreferences)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToAddTransaction = { friendId ->
                    navController.navigate(Screen.AddTransaction.createRoute(friendId))
                },
                onNavigateToFriendDetail = { friendId ->
                    navController.navigate(Screen.FriendDetail.createRoute(friendId))
                },
                onNavigateToActivity = {
                    navController.navigate(Screen.Activity.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // 3. Add Transaction Screen
        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("friendId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("transactionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getLong("friendId")?.takeIf { it > 0 }
            val transactionId = backStackEntry.arguments?.getLong("transactionId")?.takeIf { it > 0 }
            val addTxViewModel: AddTransactionViewModel = viewModel(
                factory = AddTransactionViewModelFactory(app.repository, friendId, transactionId)
            )
            AddTransactionScreen(
                viewModel = addTxViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 4. Friend Detail Screen
        composable(
            route = Screen.FriendDetail.route,
            arguments = listOf(
                navArgument("friendId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getLong("friendId") ?: -1L
            val friendDetailViewModel: FriendDetailViewModel = viewModel(
                factory = FriendDetailViewModelFactory(app.repository, friendId)
            )
            FriendDetailScreen(
                viewModel = friendDetailViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddTransaction = { fId ->
                    navController.navigate(Screen.AddTransaction.createRoute(friendId = fId))
                },
                onNavigateToEditTransaction = { fId, txId ->
                    navController.navigate(Screen.AddTransaction.createRoute(friendId = fId, transactionId = txId))
                }
            )
        }

        // 5. Activity Screen
        composable(Screen.Activity.route) {
            val activityViewModel: ActivityViewModel = viewModel(
                factory = ActivityViewModelFactory(app.repository)
            )
            ActivityScreen(
                viewModel = activityViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToFriendDetail = { friendId ->
                    navController.navigate(Screen.FriendDetail.createRoute(friendId))
                }
            )
        }

        // 6. Settings Screen
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(app.repository, app.userPreferences)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDataCleared = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
