package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.FriendWithBalance
import com.example.data.model.TransactionWithFriend
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PhittoosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

data class HomeUiState(
    val youWillGetBack: Double = 0.0,
    val youOwe: Double = 0.0,
    val netPosition: Double = 0.0,
    val openTransactionsCount: Int = 0,
    val friends: List<FriendWithBalance> = emptyList(),
    val recentActivity: List<TransactionWithFriend> = emptyList(),
    val searchQuery: String = "",
    val userName: String = "",
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: PhittoosRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val uiState: StateFlow<HomeUiState> = combine(
        repository.dashboardTotals,
        repository.friendsWithBalance,
        repository.recentActivity,
        _searchQuery
    ) { totals, friendsList, recent, query ->
        val filteredAndSortedFriends = friendsList
            .filter {
                query.isBlank() || it.friend.name.contains(query, ignoreCase = true)
            }
            .sortedWith(
                // Sorted by absolute balance (highest first)
                compareByDescending<FriendWithBalance> { abs(it.netBalance) }
                    .thenByDescending { it.lastActivityDate ?: 0L }
                    .thenBy { it.friend.name.lowercase() }
            )

        HomeUiState(
            youWillGetBack = totals.youWillGetBack,
            youOwe = totals.youOwe,
            netPosition = totals.netPosition,
            openTransactionsCount = totals.openTransactionsCount,
            friends = filteredAndSortedFriends,
            recentActivity = recent,
            searchQuery = query,
            userName = preferences.userName,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true, userName = preferences.userName)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun quickAddFriend(name: String, onFriendAdded: (Long) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.insertFriend(name.trim())
            onFriendAdded(id)
        }
    }
}

class HomeViewModelFactory(
    private val repository: PhittoosRepository,
    private val preferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
