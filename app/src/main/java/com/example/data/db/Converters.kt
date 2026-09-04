package com.example.data.db

import androidx.room.TypeConverter
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
    fun fromStatus(status: TransactionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TransactionStatus {
        return try {
            TransactionStatus.valueOf(value)
        } catch (e: Exception) {
            TransactionStatus.OPEN
        }
    }
}
