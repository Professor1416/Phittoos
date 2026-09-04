package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object AddTransaction : Screen("add_transaction?friendId={friendId}") {
        fun createRoute(friendId: Long? = null): String {
            return if (friendId != null && friendId > 0) {
                "add_transaction?friendId=$friendId"
            } else {
                "add_transaction"
            }
        }
    }
    object FriendDetail : Screen("friend_detail/{friendId}") {
        fun createRoute(friendId: Long): String = "friend_detail/$friendId"
    }
}
