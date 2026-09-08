package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Friend
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY name ASC")
    fun getAllFriends(): Flow<List<Friend>>

    @Query("SELECT * FROM friends ORDER BY name ASC")
    suspend fun getAllFriendsList(): List<Friend>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    fun getFriendById(id: Long): Flow<Friend?>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getFriendByIdOnce(id: Long): Friend?

    @Query("SELECT * FROM friends WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getFriendByName(name: String): Friend?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend): Long

    @Update
    suspend fun updateFriend(friend: Friend)

    @Delete
    suspend fun deleteFriend(friend: Friend)

    @Query("SELECT COUNT(*) FROM friends")
    fun getFriendsCount(): Flow<Int>

    @Query("SELECT f.* FROM friends f INNER JOIN transactions t ON f.id = t.friend_id GROUP BY f.id ORDER BY MAX(t.created_date) DESC LIMIT :limit")
    fun getRecentFriendsWithTransactions(limit: Int = 8): Flow<List<Friend>>

    @Query("DELETE FROM friends")
    suspend fun deleteAllFriends()
}
