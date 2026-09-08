package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityType
import com.example.data.model.ActivityWithFriend
import com.example.data.model.NeedsAttentionItem
import com.example.data.model.ReminderStage
import com.example.data.model.TransactionDirection
import com.example.data.repository.PhittoosRepository
import com.example.ui.util.Formatters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ActivityDisplayItem(
    val id: Long,
    val type: ActivityType,
    val friendId: Long,
    val friendName: String,
    val eventDescription: String,
    val formattedAmount: String,
    val formattedTime: String,
    val note: String?,
    val direction: TransactionDirection?,
    val timestamp: Long,
    val reminderStage: ReminderStage? = null
)

data class ActivityUiState(
    val needsAttentionItems: List<NeedsAttentionItem> = emptyList(),
    val groupedActivities: Map<String, List<ActivityDisplayItem>> = emptyMap(),
    val isLoading: Boolean = false,
    val totalActivityCount: Int = 0
) {
    val isEmpty: Boolean
        get() = needsAttentionItems.isEmpty() && groupedActivities.isEmpty()
}

class ActivityViewModel(
    private val repository: PhittoosRepository
) : ViewModel() {

    val uiState: StateFlow<ActivityUiState> = combine(
        repository.needsAttentionItems,
        repository.activitiesWithFriend
    ) { needsAttention, activitiesWithFriend ->
        val displayItems = activitiesWithFriend.map { mapToDisplayItem(it) }

        // Group by calendar date string ("Today", "Yesterday", "5 Sep", etc.)
        // LinkedHashMap preserves reverse-chronological group order
        val grouped = displayItems.groupBy { item ->
            Formatters.formatDate(item.timestamp)
        }

        ActivityUiState(
            needsAttentionItems = needsAttention,
            groupedActivities = grouped,
            isLoading = false,
            totalActivityCount = displayItems.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActivityUiState(isLoading = true)
    )

    companion object {
        fun mapToDisplayItem(item: ActivityWithFriend): ActivityDisplayItem {
            val act = item.activity
            val amtStr = act.amount?.let { Formatters.formatCurrency(it) } ?: ""

            val eventDescription = when (act.type) {
                ActivityType.TRANSACTION_CREATED -> {
                    when (act.direction) {
                        TransactionDirection.LENT -> "Lent $amtStr"
                        TransactionDirection.BORROWED -> "Borrowed $amtStr"
                        else -> "Added $amtStr"
                    }
                }
                ActivityType.PARTIAL_REPAYMENT -> {
                    when (act.direction) {
                        TransactionDirection.LENT -> "Received $amtStr"
                        TransactionDirection.BORROWED -> "Paid $amtStr"
                        else -> "Repaid $amtStr"
                    }
                }
                ActivityType.SETTLED -> {
                    if (act.note == "final_repayment") {
                        when (act.direction) {
                            TransactionDirection.LENT -> "Received final $amtStr · Settled"
                            TransactionDirection.BORROWED -> "Paid final $amtStr · Settled"
                            else -> "$amtStr settled"
                        }
                    } else {
                        "$amtStr settled"
                    }
                }
                ActivityType.BECAME_OVERDUE -> "$amtStr became overdue"
                ActivityType.REMINDER_SENT -> {
                    when (act.reminderStage) {
                        ReminderStage.DAY_7 -> "7-day reminder sent"
                        ReminderStage.DAY_15 -> "15-day reminder sent"
                        ReminderStage.DAY_30 -> "30-day reminder sent"
                        null -> "Reminder sent"
                    }
                }
            }

            return ActivityDisplayItem(
                id = act.id,
                type = act.type,
                friendId = act.friendId,
                friendName = item.friendName,
                eventDescription = eventDescription,
                formattedAmount = amtStr,
                formattedTime = Formatters.formatTime(act.createdAt),
                note = if (act.note != "final_repayment") act.note else null,
                direction = act.direction,
                timestamp = act.createdAt,
                reminderStage = act.reminderStage
            )
        }
    }
}

class ActivityViewModelFactory(
    private val repository: PhittoosRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            return ActivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
