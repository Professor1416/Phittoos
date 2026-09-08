package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Friend
import com.example.data.model.TransactionDirection
import com.example.data.repository.PhittoosRepository
import com.example.domain.DueDateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val errorMessage: String? = null
)

private data class FormState(
    val selectedFriend: Friend? = null,
    val friendSearchQuery: String = "",
    val direction: TransactionDirection = TransactionDirection.LENT,
    val amount: String = "",
    val note: String = "",
    val dueDate: Long? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class AddTransactionViewModel(
    private val repository: PhittoosRepository,
    initialFriendId: Long? = null
) : ViewModel() {

    private val _form = MutableStateFlow(FormState())

    init {
        if (initialFriendId != null && initialFriendId > 0) {
            viewModelScope.launch {
                repository.getFriendById(initialFriendId).collect { friend ->
                    if (friend != null && _form.value.selectedFriend == null) {
                        _form.update { it.copy(selectedFriend = friend) }
                    }
                }
            }
        }
    }

    val uiState: StateFlow<AddTransactionUiState> = combine(
        repository.allFriends,
        repository.recentFriends,
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
            errorMessage = form.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddTransactionUiState()
    )

    fun selectFriend(friend: Friend) {
        _form.update {
            it.copy(
                selectedFriend = friend,
                friendSearchQuery = "",
                errorMessage = null
            )
        }
    }

    fun updateFriendSearch(query: String) {
        _form.update { it.copy(friendSearchQuery = query) }
    }

    fun addNewFriend(name: String) {
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
            repository.addTransaction(
                friendId = friend.id,
                amount = amountVal,
                direction = dir,
                note = currentForm.note,
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

class AddTransactionViewModelFactory(
    private val repository: PhittoosRepository,
    private val friendId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) {
            return AddTransactionViewModel(repository, friendId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
