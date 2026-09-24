package com.professor1416.phittoos.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object AddTransaction : Screen("add_transaction?friendId={friendId}&transactionId={transactionId}") {
        fun createRoute(friendId: Long? = null, transactionId: Long? = null): String {
            val params = mutableListOf<String>()
            if (friendId != null && friendId > 0) {
                params.add("friendId=$friendId")
            }
            if (transactionId != null && transactionId > 0) {
                params.add("transactionId=$transactionId")
            }
            return if (params.isNotEmpty()) {
                "add_transaction?${params.joinToString("&")}"
            } else {
                "add_transaction"
            }
        }
    }
    object FriendDetail : Screen("friend_detail/{friendId}") {
        fun createRoute(friendId: Long): String = "friend_detail/$friendId"
    }
    object Activity : Screen("activity")
    object Settings : Screen("settings")
}
