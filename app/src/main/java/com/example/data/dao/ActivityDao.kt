package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Query("SELECT * FROM activity_records ORDER BY created_at DESC, id DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity_records ORDER BY created_at DESC, id DESC")
    suspend fun getAllActivitiesList(): List<ActivityEntity>

    @Query("SELECT * FROM activity_records WHERE friend_id = :friendId ORDER BY created_at DESC, id DESC")
    fun getActivitiesForFriend(friendId: Long): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity_records WHERE friend_id = :friendId ORDER BY created_at DESC, id DESC")
    suspend fun getActivitiesForFriendDirect(friendId: Long): List<ActivityEntity>

    @Query("SELECT * FROM activity_records ORDER BY created_at DESC, id DESC LIMIT :limit")
    fun getRecentActivities(limit: Int): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity_records WHERE id = :id")
    suspend fun getActivityById(id: Long): ActivityEntity?

    @Query("SELECT COUNT(*) FROM activity_records")
    suspend fun getActivityCount(): Int

    @Query("DELETE FROM activity_records")
    suspend fun deleteAllActivities()
}
