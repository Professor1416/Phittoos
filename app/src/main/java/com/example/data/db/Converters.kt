package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.ActivityType
import com.example.data.model.ReminderStage
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionStatus

class Converters {
    @TypeConverter
    fun fromDirection(direction: TransactionDirection): String = direction.name

    @TypeConverter
    fun toDirection(value: String): TransactionDirection {
        return try {
            TransactionDirection.valueOf(value)
        } catch (e: Exception) {
            TransactionDirection.LENT
        }
    }

    @TypeConverter
    fun fromNullableDirection(direction: TransactionDirection?): String? = direction?.name

    @TypeConverter
    fun toNullableDirection(value: String?): TransactionDirection? {
        if (value == null) return null
        return try {
            TransactionDirection.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromStatus(status: TransactionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TransactionStatus {
        return try {
            TransactionStatus.valueOf(value)
        } catch (e: Exception) {
            TransactionStatus.OPEN
        }
    }

    @TypeConverter
    fun fromActivityType(type: ActivityType): String = type.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType {
        return try {
            ActivityType.valueOf(value)
        } catch (e: Exception) {
            ActivityType.TRANSACTION_CREATED
        }
    }

    @TypeConverter
    fun fromReminderStage(value: ReminderStage?): String? = value?.name

    @TypeConverter
    fun toReminderStage(value: String?): ReminderStage? {
        if (value == null) return null
        return try {
            ReminderStage.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }
}
