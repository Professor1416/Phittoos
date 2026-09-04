package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "contact_info")
    val contactInfo: String? = null,
    @ColumnInfo(name = "reliability_tier")
    val reliabilityTier: String = "NEW"
)
