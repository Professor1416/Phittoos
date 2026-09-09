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
import com.example.domain.ReliabilityEngine
import com.example.domain.ReliabilityInfo
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
    val isFriendNotFound: Boolean = false,
    val toastMessage: String? = null,
    val bulkSettlementEligibility: BulkSettlementEligibility = BulkSettlementEligibility.NONE,
    val bulkSettlementTotalRemaining: Double = 0.0,
    val bulkSettlementOpenCount: Int = 0,
    val settlingTransactionIds: Set<Long> = emptySet(),
    val isBulkSettling: Boolean = false,
    val reliabilityInfo: ReliabilityInfo = ReliabilityInfo.NEW
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

        val reliability = ReliabilityEngine.calculate(transactions)

        FriendDetailUiState(
            friend = friend,
            netBalance = net,
            timeline = transactions,
            openTransactionsCount = openCount,
            isLoading = false,
            isFriendNotFound = friend == null,
            toastMessage = toast,
            bulkSettlementEligibility = eligibility,
            bulkSettlementTotalRemaining = bulkRemaining,
            bulkSettlementOpenCount = bulkCount,
            settlingTransactionIds = settlingIds,
            isBulkSettling = isBulkSettling,
            reliabilityInfo = reliability
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
                    _toastMessage.value = "Marked as fully paid."
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
                            _toastMessage.value = "Final repayment of $formatted recorded."
                        } else {
                            _toastMessage.value = "Repayment of $formatted recorded."
                        }
                        onSuccess?.invoke()
                    }
                    is RepaymentResult.Error -> {
                        val errorMsg = resolveRepaymentErrorMessage(result)
                        _toastMessage.value = errorMsg
                        onError?.invoke(errorMsg)
                    }
                }
            } finally {
                _settlingTransactionIds.update { it - transactionId }
            }
        }
    }

    private fun resolveRepaymentErrorMessage(error: RepaymentResult.Error): String {
        return when (error.reason) {
            com.example.data.repository.RepaymentErrorReason.INVALID_AMOUNT ->
                "Enter an amount greater than ₹0."
            com.example.data.repository.RepaymentErrorReason.TRANSACTION_NOT_FOUND ->
                "This transaction could not be found."
            com.example.data.repository.RepaymentErrorReason.ALREADY_SETTLED ->
                "This transaction is already fully paid."
            com.example.data.repository.RepaymentErrorReason.EXCEEDS_REMAINING -> {
                val remaining = error.remainingAmount
                if (remaining != null) {
                    "Enter ${Formatters.formatCurrency(remaining)} or less. That’s the amount left to pay."
                } else {
                    error.message
                }
            }
            com.example.data.repository.RepaymentErrorReason.NO_LONGER_OPEN ->
                "This transaction is no longer pending."
        }
    }

    fun settleAllSameDirection() {
        if (_isBulkSettling.value) return

        _isBulkSettling.value = true
        viewModelScope.launch {
            try {
                val success = repository.settleAllSameDirectionForFriend(friendId)
                if (success) {
                    _toastMessage.value = "Selected transactions marked as fully paid."
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
