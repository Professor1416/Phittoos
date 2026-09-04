package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Friend
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.repository.PhittoosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FriendDetailUiState(
    val friend: Friend? = null,
    val netBalance: Double = 0.0, // >0: owes user, <0: user owes, 0: settled
    val timeline: List<TransactionEntity> = emptyList(),
    val openTransactionsCount: Int = 0,
    val isLoading: Boolean = true,
    val toastMessage: String? = null
)

class FriendDetailViewModel(
    private val repository: PhittoosRepository,
    private val friendId: Long
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FriendDetailUiState> = combine(
        repository.getFriendById(friendId),
        repository.getTransactionsForFriend(friendId),
        _toastMessage
    ) { friend, transactions, toast ->
        var net = 0.0
        var openCount = 0

        for (tx in transactions) {
            if (tx.status == TransactionStatus.OPEN) {
                openCount++
                val effectiveAmount = tx.amount - (tx.paidAmount ?: 0.0)
                if (tx.direction == TransactionDirection.LENT) {
                    net += effectiveAmount
                } else {
                    net -= effectiveAmount
                }
            }
        }

        FriendDetailUiState(
            friend = friend,
            netBalance = net,
            timeline = transactions,
            openTransactionsCount = openCount,
            isLoading = friend == null,
            toastMessage = toast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FriendDetailUiState()
    )

    fun markAllAsPaid() {
        viewModelScope.launch {
            repository.markAllForFriendAsPaid(friendId)
            _toastMessage.value = "All balance marked as settled (Phittoos!)"
        }
    }

    fun markTransactionAsPaid(transactionId: Long) {
        viewModelScope.launch {
            repository.markTransactionAsPaid(transactionId)
            _toastMessage.value = "Transaction marked as settled"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}

class FriendDetailViewModelFactory(
    private val repository: PhittoosRepository,
    private val friendId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendDetailViewModel::class.java)) {
            return FriendDetailViewModel(repository, friendId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
