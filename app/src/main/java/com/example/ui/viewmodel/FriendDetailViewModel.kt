package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Friend
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.effectiveRemainingAmount
import com.example.data.repository.PhittoosRepository
import com.example.data.repository.RepaymentResult
import com.example.ui.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BulkSettlementEligibility {
    NONE,
    SAME_DIRECTION_LENT,
    SAME_DIRECTION_BORROWED,
    MIXED_DIRECTIONS
}

data class FriendDetailUiState(
    val friend: Friend? = null,
    val netBalance: Double = 0.0, // >0: owes user, <0: user owes, 0: settled
    val timeline: List<TransactionEntity> = emptyList(),
    val openTransactionsCount: Int = 0,
    val isLoading: Boolean = true,
    val toastMessage: String? = null,
    val bulkSettlementEligibility: BulkSettlementEligibility = BulkSettlementEligibility.NONE,
    val bulkSettlementTotalRemaining: Double = 0.0,
    val bulkSettlementOpenCount: Int = 0,
    val settlingTransactionIds: Set<Long> = emptySet(),
    val isBulkSettling: Boolean = false
)

class FriendDetailViewModel(
    private val repository: PhittoosRepository,
    private val friendId: Long
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    private val _settlingTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isBulkSettling = MutableStateFlow(false)

    val uiState: StateFlow<FriendDetailUiState> = combine(
        repository.getFriendById(friendId),
        repository.getTransactionsForFriend(friendId),
        _toastMessage,
        _settlingTransactionIds,
        _isBulkSettling
    ) { friend, transactions, toast, settlingIds, isBulkSettling ->
        val openTxs = transactions.filter { it.status == TransactionStatus.OPEN }
        val openCount = openTxs.size

        var net = 0.0
        for (tx in openTxs) {
            val effectiveAmount = tx.effectiveRemainingAmount
            if (tx.direction == TransactionDirection.LENT) {
                net += effectiveAmount
            } else {
                net -= effectiveAmount
            }
        }

        val hasLent = openTxs.any { it.direction == TransactionDirection.LENT }
        val hasBorrowed = openTxs.any { it.direction == TransactionDirection.BORROWED }

        val eligibility: BulkSettlementEligibility
        val bulkRemaining: Double
        val bulkCount: Int

        when {
            openTxs.isEmpty() -> {
                eligibility = BulkSettlementEligibility.NONE
                bulkRemaining = 0.0
                bulkCount = 0
            }
            hasLent && hasBorrowed -> {
                eligibility = BulkSettlementEligibility.MIXED_DIRECTIONS
                bulkRemaining = 0.0
                bulkCount = openCount
            }
            hasLent -> {
                eligibility = BulkSettlementEligibility.SAME_DIRECTION_LENT
                bulkRemaining = openTxs.sumOf { it.effectiveRemainingAmount }
                bulkCount = openCount
            }
            else -> {
                eligibility = BulkSettlementEligibility.SAME_DIRECTION_BORROWED
                bulkRemaining = openTxs.sumOf { it.effectiveRemainingAmount }
                bulkCount = openCount
            }
        }

        FriendDetailUiState(
            friend = friend,
            netBalance = net,
            timeline = transactions,
            openTransactionsCount = openCount,
            isLoading = friend == null,
            toastMessage = toast,
            bulkSettlementEligibility = eligibility,
            bulkSettlementTotalRemaining = bulkRemaining,
            bulkSettlementOpenCount = bulkCount,
            settlingTransactionIds = settlingIds,
            isBulkSettling = isBulkSettling
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FriendDetailUiState()
    )

    fun settleTransaction(transactionId: Long) {
        if (_settlingTransactionIds.value.contains(transactionId)) return
        _settlingTransactionIds.update { it + transactionId }
        viewModelScope.launch {
            try {
                val tx = repository.getTransactionById(transactionId)
                if (tx != null && tx.status == TransactionStatus.OPEN) {
                    repository.markTransactionAsPaid(transactionId)
                    _toastMessage.value = "Transaction marked as settled"
                }
            } finally {
                _settlingTransactionIds.update { it - transactionId }
            }
        }
    }

    fun recordRepayment(
        transactionId: Long,
        amount: Double,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_settlingTransactionIds.value.contains(transactionId)) return
        _settlingTransactionIds.update { it + transactionId }
        viewModelScope.launch {
            try {
                val result = repository.recordRepayment(transactionId, amount)
                when (result) {
                    is RepaymentResult.Success -> {
                        val formatted = Formatters.formatCurrency(amount)
                        if (result.isFullySettled) {
                            _toastMessage.value = "Transaction fully settled! ($formatted)"
                        } else {
                            _toastMessage.value = "Recorded repayment of $formatted"
                        }
                        onSuccess?.invoke()
                    }
                    is RepaymentResult.Error -> {
                        _toastMessage.value = result.message
                        onError?.invoke(result.message)
                    }
                }
            } finally {
                _settlingTransactionIds.update { it - transactionId }
            }
        }
    }

    fun settleAllSameDirection() {
        if (_isBulkSettling.value) return

        _isBulkSettling.value = true
        viewModelScope.launch {
            try {
                val success = repository.settleAllSameDirectionForFriend(friendId)
                if (success) {
                    _toastMessage.value = "All eligible transactions marked as settled"
                }
            } finally {
                _isBulkSettling.value = false
            }
        }
    }

    fun markAllAsPaid() {
        settleAllSameDirection()
    }

    fun markTransactionAsPaid(transactionId: Long) {
        settleTransaction(transactionId)
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
