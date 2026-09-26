package com.professor1416.phittoos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.professor1416.phittoos.data.model.Friend
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.model.effectivePaidAmount
import com.professor1416.phittoos.data.repository.EditErrorReason
import com.professor1416.phittoos.data.repository.EditTransactionResult
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.domain.DueDateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddTransactionUiState(
    val recentFriends: List<Friend> = emptyList(),
    val allFriends: List<Friend> = emptyList(),
    val selectedFriend: Friend? = null,
    val friendSearchQuery: String = "",
    val direction: TransactionDirection = TransactionDirection.LENT,
    val amount: String = "",
    val note: String = "",
    val dueDate: Long? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isFriendLocked: Boolean = false,
    val isEditMode: Boolean = false,
    val isFriendNotFound: Boolean = false,
    val editTransactionId: Long? = null
)

private data class FormState(
    val selectedFriend: Friend? = null,
    val friendSearchQuery: String = "",
    val direction: TransactionDirection = TransactionDirection.LENT,
    val amount: String = "",
    val note: String = "",
    val dueDate: Long? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isFriendLocked: Boolean = false,
    val isEditMode: Boolean = false,
    val isFriendNotFound: Boolean = false,
    val editTransactionId: Long? = null
)

class AddTransactionViewModel(
    private val repository: PhittoosRepository,
    initialFriendId: Long? = null,
    transactionIdToEdit: Long? = null
) : ViewModel() {

    private val _form = MutableStateFlow(
        FormState(
            isFriendLocked = (initialFriendId != null && initialFriendId > 0) || (transactionIdToEdit != null && transactionIdToEdit > 0),
            isEditMode = transactionIdToEdit != null && transactionIdToEdit > 0,
            editTransactionId = transactionIdToEdit?.takeIf { it > 0 }
        )
    )

    init {
        if (transactionIdToEdit != null && transactionIdToEdit > 0) {
            viewModelScope.launch {
                val tx = repository.getTransactionById(transactionIdToEdit)
                if (tx == null) {
                    _form.update { it.copy(errorMessage = "This transaction could not be found.") }
                    return@launch
                }
                if (tx.status != TransactionStatus.OPEN) {
                    _form.update { it.copy(errorMessage = "Settled transactions are kept in history and can't be directly changed.") }
                    return@launch
                }
                if (tx.effectivePaidAmount > 0.0 || (tx.paidAmount ?: 0.0) > 0.0 || tx.settledAt != null) {
                    _form.update { it.copy(errorMessage = "This transaction has repayment history and can't be directly changed.") }
                    return@launch
                }

                val friend = repository.getFriendByIdOnce(tx.friendId)
                if (friend == null) {
                    _form.update { it.copy(isFriendNotFound = true, errorMessage = "Friend not found.") }
                    return@launch
                }

                val formattedAmount = if (tx.amount % 1.0 == 0.0) {
                    tx.amount.toLong().toString()
                } else {
                    tx.amount.toString()
                }

                _form.update {
                    it.copy(
                        selectedFriend = friend,
                        direction = tx.direction,
                        amount = formattedAmount,
                        note = tx.note ?: "",
                        dueDate = tx.dueDate,
                        isFriendLocked = true,
                        isEditMode = true,
                        editTransactionId = tx.id
                    )
                }
            }
        } else if (initialFriendId != null && initialFriendId > 0) {
            viewModelScope.launch {
                val friend = repository.getFriendByIdOnce(initialFriendId)
                if (friend != null) {
                    _form.update {
                        it.copy(
                            selectedFriend = friend,
                            isFriendLocked = true,
                            isFriendNotFound = false
                        )
                    }
                } else {
                    _form.update {
                        it.copy(
                            selectedFriend = null,
                            isFriendLocked = true,
                            isFriendNotFound = true,
                            errorMessage = "Friend not found."
                        )
                    }
                }
            }
        }
    }

    val uiState: StateFlow<AddTransactionUiState> = combine(
        repository.allFriends.onStart { emit(emptyList()) },
        repository.recentFriends.onStart { emit(emptyList()) },
        _form
    ) { allFriends, recentFriends, form ->
        val effectiveRecent = if (recentFriends.isNotEmpty()) {
            recentFriends
        } else {
            allFriends.take(6)
        }

        AddTransactionUiState(
            recentFriends = effectiveRecent,
            allFriends = allFriends,
            selectedFriend = form.selectedFriend,
            friendSearchQuery = form.friendSearchQuery,
            direction = form.direction,
            amount = form.amount,
            note = form.note,
            dueDate = form.dueDate,
            isSaving = form.isSaving,
            errorMessage = form.errorMessage,
            isFriendLocked = form.isFriendLocked,
            isEditMode = form.isEditMode,
            isFriendNotFound = form.isFriendNotFound,
            editTransactionId = form.editTransactionId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AddTransactionUiState(
            isFriendLocked = (initialFriendId != null && initialFriendId > 0) || (transactionIdToEdit != null && transactionIdToEdit > 0),
            isEditMode = transactionIdToEdit != null && transactionIdToEdit > 0,
            editTransactionId = transactionIdToEdit?.takeIf { it > 0 }
        )
    )

    fun selectFriend(friend: Friend) {
        if (_form.value.isFriendLocked) return
        _form.update {
            it.copy(
                selectedFriend = friend,
                friendSearchQuery = "",
                errorMessage = null
            )
        }
    }

    fun updateFriendSearch(query: String) {
        if (_form.value.isFriendLocked) return
        _form.update { it.copy(friendSearchQuery = query) }
    }

    fun addNewFriend(name: String) {
        if (_form.value.isFriendLocked) return
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val id = repository.insertFriend(trimmed)
            _form.update {
                it.copy(
                    selectedFriend = Friend(id = id, name = trimmed),
                    friendSearchQuery = "",
                    errorMessage = null
                )
            }
        }
    }

    fun setDirection(dir: TransactionDirection) {
        _form.update { it.copy(direction = dir) }
    }

    fun setAmount(newAmount: String) {
        val clean = newAmount.filter { it.isDigit() || it == '.' }
        if (clean.count { it == '.' } <= 1) {
            _form.update { it.copy(amount = clean, errorMessage = null) }
        }
    }

    fun setNote(newNote: String) {
        _form.update { it.copy(note = newNote) }
    }

    fun setDueDate(dateMillis: Long?) {
        if (dateMillis != null && DueDateHelper.isPastDate(dateMillis)) {
            _form.update {
                it.copy(
                    dueDate = dateMillis,
                    errorMessage = "Due date cannot be in the past."
                )
            }
        } else {
            _form.update {
                it.copy(
                    dueDate = dateMillis,
                    errorMessage = if (it.errorMessage == "Due date cannot be in the past.") null else it.errorMessage
                )
            }
        }
    }

    fun saveTransaction(onSuccess: (toastMessage: String) -> Unit) {
        val currentForm = _form.value
        if (currentForm.isSaving) return

        if (currentForm.isFriendNotFound) {
            _form.update { it.copy(errorMessage = "Friend not found.") }
            return
        }

        val friend = currentForm.selectedFriend
        if (friend == null) {
            _form.update { it.copy(errorMessage = "Please select or add a friend") }
            return
        }

        val amountVal = currentForm.amount.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0) {
            _form.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }

        if (currentForm.dueDate != null && DueDateHelper.isPastDate(currentForm.dueDate)) {
            _form.update { it.copy(errorMessage = "Due date cannot be in the past.") }
            return
        }

        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            val dir = currentForm.direction
            val trimmedNote = currentForm.note.trim().ifBlank { null }

            if (currentForm.isEditMode && currentForm.editTransactionId != null) {
                val result = repository.updateOpenUnpaidTransaction(
                    transactionId = currentForm.editTransactionId,
                    amount = amountVal,
                    direction = dir,
                    note = trimmedNote,
                    dueDate = currentForm.dueDate
                )
                _form.update { it.copy(isSaving = false) }
                when (result) {
                    is EditTransactionResult.Success -> {
                        onSuccess("Changes saved.")
                    }
                    is EditTransactionResult.Error -> {
                        val msg = when (result.reason) {
                            EditErrorReason.TRANSACTION_NOT_FOUND -> "This transaction could not be found."
                            EditErrorReason.NOT_OPEN -> "This transaction is no longer pending."
                            EditErrorReason.HAS_REPAYMENTS -> "This transaction has repayment history and can't be directly changed."
                            EditErrorReason.INVALID_AMOUNT -> "Enter an amount greater than ₹0."
                            EditErrorReason.INVALID_DUE_DATE -> "Due date cannot be in the past."
                        }
                        _form.update { it.copy(errorMessage = msg) }
                    }
                }
            } else {
                repository.addTransaction(
                    friendId = friend.id,
                    amount = amountVal,
                    direction = dir,
                    note = trimmedNote,
                    dueDate = currentForm.dueDate
                )
                _form.update { it.copy(isSaving = false) }

                val formattedAmount = if (amountVal % 1.0 == 0.0) {
                    amountVal.toInt().toString()
                } else {
                    String.format("%.2f", amountVal)
                }

                val toast = if (dir == TransactionDirection.LENT) {
                    "Added: You lent ₹$formattedAmount to ${friend.name}"
                } else {
                    "Added: You borrowed ₹$formattedAmount from ${friend.name}"
                }
                onSuccess(toast)
            }
        }
    }
}

class AddTransactionViewModelFactory(
    private val repository: PhittoosRepository,
    private val friendId: Long? = null,
    private val transactionId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) {
            return AddTransactionViewModel(repository, friendId, transactionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
