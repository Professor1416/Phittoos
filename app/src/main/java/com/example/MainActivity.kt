package com.example

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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.reminder.ReminderNotificationHelper
import com.example.ui.navigation.Screen
import com.example.ui.screens.activity.ActivityScreen
import com.example.ui.screens.addtransaction.AddTransactionScreen
import com.example.ui.screens.frienddetail.FriendDetailScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.theme.PhittoosTheme
import com.example.ui.viewmodel.ActivityViewModel
import com.example.ui.viewmodel.ActivityViewModelFactory
import com.example.ui.viewmodel.AddTransactionViewModel
import com.example.ui.viewmodel.AddTransactionViewModelFactory
import com.example.ui.viewmodel.FriendDetailViewModel
import com.example.ui.viewmodel.FriendDetailViewModelFactory
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PhittoosApplication
        val friendIdFromNotification = intent?.getLongExtra(ReminderNotificationHelper.EXTRA_FRIEND_ID, -1L)?.takeIf { it > 0 }

        setContent {
            PhittoosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
    val context = LocalContext.current

    // Request notification permission once on Android 13+ contextually
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* no-op: app works regardless of grant/denial */ }

        LaunchedEffect(Unit) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(initialFriendId) {
        if (initialFriendId != null && userPreferences.hasCompletedOnboarding) {
            navController.navigate(Screen.FriendDetail.createRoute(initialFriendId))
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
                }
            )
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getLong("friendId")?.takeIf { it > 0 }
            val addTxViewModel: AddTransactionViewModel = viewModel(
                factory = AddTransactionViewModelFactory(app.repository, friendId)
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
                    navController.navigate(Screen.AddTransaction.createRoute(fId))
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
    }
}
