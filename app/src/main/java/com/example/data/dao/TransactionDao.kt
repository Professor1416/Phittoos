package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY created_date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY created_date ASC, id ASC")
    suspend fun getAllTransactionsOrdered(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE friend_id = :friendId ORDER BY created_date DESC")
    fun getTransactionsForFriend(friendId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY created_date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE friend_id = :friendId AND status = 'OPEN'")
    suspend fun getOpenTransactionsForFriend(friendId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status = 'OPEN' AND due_date IS NOT NULL ORDER BY due_date ASC")
    fun getOpenTransactionsWithDueDate(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'OPEN' AND due_date IS NOT NULL ORDER BY due_date ASC")
    suspend fun getOpenTransactionsWithDueDateList(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET paid_amount = :newPaidAmount, status = :newStatus, settled_at = :settledAt WHERE id = :id AND status = 'OPEN'")
    suspend fun updateRepayment(id: Long, newPaidAmount: Double, newStatus: TransactionStatus, settledAt: Long?): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE friend_id = :friendId AND status = 'OPEN'")
    suspend fun getOpenCountForFriend(friendId: Long): Int

    @Query("UPDATE transactions SET status = 'CONFIRMED', paid_amount = amount, settled_at = :settledAt WHERE id = :id AND status = 'OPEN'")
    suspend fun markAsConfirmed(id: Long, settledAt: Long): Int

    @Query("UPDATE transactions SET status = 'CONFIRMED', paid_amount = amount, settled_at = :settledAt WHERE friend_id = :friendId AND status = 'OPEN'")
    suspend fun markAllOpenForFriendAsConfirmed(friendId: Long, settledAt: Long): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
