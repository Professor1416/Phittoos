package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.ActivityType
import com.example.data.model.ActivityWithFriend
import com.example.data.model.NeedsAttentionItem
import com.example.data.model.ReminderStage
import com.example.data.model.TransactionDirection
import com.example.data.repository.PhittoosRepository
import com.example.ui.util.Formatters
import com.example.ui.util.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ActivityDisplayItem(
    val id: Long,
    val type: ActivityType,
    val friendId: Long,
    val friendName: String,
    val eventDescription: UiMessage,
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

    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    fun refreshDateGrouping(nowMillis: Long = System.currentTimeMillis()) {
        refreshTrigger.value = nowMillis
    }

    val uiState: StateFlow<ActivityUiState> = combine(
        repository.needsAttentionItems,
        repository.activitiesWithFriend,
        refreshTrigger
    ) { needsAttention, activitiesWithFriend, nowMillis ->
        val displayItems = activitiesWithFriend.map { mapToDisplayItem(it) }

        // Group by calendar date string ("Today", "Yesterday", "5 Sep", etc.)
        // LinkedHashMap preserves reverse-chronological group order
        val grouped = displayItems.groupBy { item ->
            Formatters.formatDate(item.timestamp, nowMillis = nowMillis)
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
                        TransactionDirection.LENT -> UiMessage(R.string.activity_event_lent_created, amtStr)
                        TransactionDirection.BORROWED -> UiMessage(R.string.activity_event_borrowed_created, amtStr)
                        else -> UiMessage(R.string.activity_event_added, amtStr)
                    }
                }
                ActivityType.PARTIAL_REPAYMENT -> {
                    when (act.direction) {
                        TransactionDirection.LENT -> UiMessage(R.string.activity_event_lent_partial, amtStr)
                        TransactionDirection.BORROWED -> UiMessage(R.string.activity_event_borrowed_partial, amtStr)
                        else -> UiMessage(R.string.activity_event_repaid, amtStr)
                    }
                }
                ActivityType.SETTLED -> {
                    if (act.note == "final_repayment") {
                        when (act.direction) {
                            TransactionDirection.LENT -> UiMessage(R.string.activity_event_lent_final, amtStr)
                            TransactionDirection.BORROWED -> UiMessage(R.string.activity_event_borrowed_final, amtStr)
                            else -> UiMessage(R.string.activity_event_manual_settled, amtStr)
                        }
                    } else {
                        UiMessage(R.string.activity_event_manual_settled, amtStr)
                    }
                }
                ActivityType.BECAME_OVERDUE -> UiMessage(R.string.activity_event_overdue, amtStr)
                ActivityType.REMINDER_SENT -> {
                    when (act.reminderStage) {
                        ReminderStage.DAY_7 -> UiMessage(R.string.activity_reminder_day_7)
                        ReminderStage.DAY_15 -> UiMessage(R.string.activity_reminder_day_15)
                        ReminderStage.DAY_30 -> UiMessage(R.string.activity_reminder_day_30)
                        null -> UiMessage(R.string.activity_reminder_fallback)
                    }
                }
            }

            val isSystemGeneratedNote = act.note == "final_repayment" ||
                act.type == ActivityType.REMINDER_SENT ||
                act.reminderStage != null ||
                act.note?.endsWith("reminder sent", ignoreCase = true) == true

            return ActivityDisplayItem(
                id = act.id,
                type = act.type,
                friendId = act.friendId,
                friendName = item.friendName,
                eventDescription = eventDescription,
                formattedAmount = amtStr,
                formattedTime = Formatters.formatTime(act.createdAt),
                note = if (isSystemGeneratedNote) null else act.note,
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
