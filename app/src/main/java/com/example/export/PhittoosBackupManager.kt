package com.example.export

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.ActivityType
import com.example.data.model.Friend
import com.example.data.model.ReminderStage
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PhittoosRepository
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PhittoosBackupManager {

    private const val BACKUP_VERSION = 1

    fun generateDefaultFileName(now: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return "phittoos-backup-${sdf.format(Date(now))}.json"
    }

    suspend fun exportBackup(
        repository: PhittoosRepository,
        userPreferences: UserPreferences
    ): String {
        val root = JSONObject()

        // 1. Metadata
        val metadata = JSONObject().apply {
            put("backupVersion", BACKUP_VERSION)
            put("appVersion", "1.0.0")
            put("createdAt", System.currentTimeMillis())
        }
        root.put("metadata", metadata)

        // 2. Preferences
        val prefsJson = JSONObject().apply {
            put("userName", userPreferences.userName)
            put("hasCompletedOnboarding", userPreferences.hasCompletedOnboarding)
            put("remindersEnabled", userPreferences.remindersEnabled)
        }
        root.put("preferences", prefsJson)

        // 3. Friends
        val friends = repository.getAllFriendsForExport()
        val friendsArray = JSONArray()
        for (friend in friends) {
            val friendJson = JSONObject().apply {
                put("id", friend.id)
                put("name", friend.name)
                put("contactInfo", friend.contactInfo ?: JSONObject.NULL)
                put("reliabilityTier", friend.reliabilityTier)
            }
            friendsArray.put(friendJson)
        }
        root.put("friends", friendsArray)

        // 4. Transactions
        val transactions = repository.getAllTransactionsForExport()
        val txsArray = JSONArray()
        for (tx in transactions) {
            val txJson = JSONObject().apply {
                put("id", tx.id)
                put("friendId", tx.friendId)
                put("amount", tx.amount)
                put("direction", tx.direction.name)
                put("note", tx.note ?: JSONObject.NULL)
                put("createdDate", tx.createdDate)
                put("dueDate", tx.dueDate ?: JSONObject.NULL)
                put("status", tx.status.name)
                put("paidAmount", tx.paidAmount ?: JSONObject.NULL)
                put("settledAt", tx.settledAt ?: JSONObject.NULL)
            }
            txsArray.put(txJson)
        }
        root.put("transactions", txsArray)

        // 5. Activities
        val activities = repository.allActivitiesList()
        val activitiesArray = JSONArray()
        for (act in activities) {
            val actJson = JSONObject().apply {
                put("id", act.id)
                put("type", act.type.name)
                put("friendId", act.friendId)
                put("transactionId", act.transactionId ?: JSONObject.NULL)
                put("amount", act.amount ?: JSONObject.NULL)
                put("direction", act.direction?.name ?: JSONObject.NULL)
                put("note", act.note ?: JSONObject.NULL)
                put("reminderStage", act.reminderStage?.name ?: JSONObject.NULL)
                put("createdAt", act.createdAt)
            }
            activitiesArray.put(actJson)
        }
        root.put("activities", activitiesArray)

        return root.toString(2)
    }

    suspend fun restoreBackup(
        jsonString: String,
        repository: PhittoosRepository,
        userPreferences: UserPreferences,
        database: AppDatabase
    ): Result<Unit> {
        return try {
            val root = JSONObject(jsonString)

            // 1. Parse & Validate Metadata
            if (!root.has("metadata")) {
                return Result.failure(Exception("Invalid backup: missing 'metadata' section."))
            }
            val metadata = root.getJSONObject("metadata")
            val backupVersion = metadata.optInt("backupVersion", -1)
            if (backupVersion == -1) {
                return Result.failure(Exception("Invalid backup: missing 'backupVersion'."))
            }
            if (backupVersion > BACKUP_VERSION) {
                return Result.failure(Exception("Unsupported backup version: $backupVersion. (Current supported version is $BACKUP_VERSION)."))
            }

            // 2. Parse & Validate Preferences
            if (!root.has("preferences")) {
                return Result.failure(Exception("Invalid backup: missing 'preferences' section."))
            }
            val prefsJson = root.getJSONObject("preferences")
            val userName = prefsJson.optString("userName", "")
            val hasCompletedOnboarding = prefsJson.optBoolean("hasCompletedOnboarding", false)
            val remindersEnabled = prefsJson.optBoolean("remindersEnabled", true)

            // 3. Parse & Validate Friends
            if (!root.has("friends")) {
                return Result.failure(Exception("Invalid backup: missing 'friends' list."))
            }
            val friendsArray = root.getJSONArray("friends")
            val friendIds = mutableSetOf<Long>()
            val friendsList = ArrayList<Friend>()
            for (i in 0 until friendsArray.length()) {
                val fJson = friendsArray.getJSONObject(i)
                val id = fJson.optLong("id", -1)
                if (id == -1L) {
                    return Result.failure(Exception("Friend record at position $i lacks a valid 'id'."))
                }
                val name = fJson.optString("name", "")
                if (name.isBlank()) {
                    return Result.failure(Exception("Friend record at position $i has an empty name."))
                }
                val contactInfo = if (fJson.isNull("contactInfo")) null else fJson.getString("contactInfo")
                val reliabilityTier = fJson.optString("reliabilityTier", "NEW")

                friendIds.add(id)
                friendsList.add(
                    Friend(
                        id = id,
                        name = name,
                        contactInfo = contactInfo,
                        reliabilityTier = reliabilityTier
                    )
                )
            }

            // 4. Parse & Validate Transactions
            if (!root.has("transactions")) {
                return Result.failure(Exception("Invalid backup: missing 'transactions' list."))
            }
            val txsArray = root.getJSONArray("transactions")
            val transactionIds = mutableSetOf<Long>()
            val transactionsList = ArrayList<TransactionEntity>()
            for (i in 0 until txsArray.length()) {
                val tJson = txsArray.getJSONObject(i)
                val id = tJson.optLong("id", -1)
                if (id == -1L) {
                    return Result.failure(Exception("Transaction record at position $i lacks a valid 'id'."))
                }
                val friendId = tJson.optLong("friendId", -1)
                if (friendId == -1L) {
                    return Result.failure(Exception("Transaction $id lacks a 'friendId'."))
                }
                if (friendId !in friendIds) {
                    return Result.failure(Exception("Referential Integrity Check Failed: Transaction $id refers to non-existent Friend $friendId."))
                }

                val amount = tJson.optDouble("amount", -1.0)
                if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) {
                    return Result.failure(Exception("Transaction $id has an invalid amount: $amount."))
                }

                val directionStr = tJson.optString("direction", "")
                val direction = try {
                    TransactionDirection.valueOf(directionStr)
                } catch (e: Exception) {
                    return Result.failure(Exception("Transaction $id has an invalid direction value: '$directionStr'."))
                }

                val statusStr = tJson.optString("status", "")
                val status = try {
                    TransactionStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    return Result.failure(Exception("Transaction $id has an invalid status value: '$statusStr'."))
                }

                val paidAmount = if (tJson.isNull("paidAmount")) null else tJson.getDouble("paidAmount")
                if (paidAmount != null) {
                    if (paidAmount < 0.0 || paidAmount.isNaN() || paidAmount.isInfinite()) {
                        return Result.failure(Exception("Transaction $id has an invalid paidAmount: $paidAmount."))
                    }
                    if (paidAmount > amount) {
                        return Result.failure(Exception("Transaction $id has paidAmount ($paidAmount) exceeding the principal amount ($amount)."))
                    }
                }

                val note = if (tJson.isNull("note")) null else tJson.getString("note")
                val createdDate = tJson.optLong("createdDate", System.currentTimeMillis())
                val dueDate = if (tJson.isNull("dueDate")) null else tJson.getLong("dueDate")
                val settledAt = if (tJson.isNull("settledAt")) null else tJson.getLong("settledAt")

                transactionIds.add(id)
                transactionsList.add(
                    TransactionEntity(
                        id = id,
                        friendId = friendId,
                        amount = amount,
                        direction = direction,
                        note = note,
                        createdDate = createdDate,
                        dueDate = dueDate,
                        status = status,
                        paidAmount = paidAmount,
                        settledAt = settledAt
                    )
                )
            }

            // 5. Parse & Validate Activities
            val activitiesList = ArrayList<ActivityEntity>()
            if (root.has("activities")) {
                val actsArray = root.getJSONArray("activities")
                for (i in 0 until actsArray.length()) {
                    val aJson = actsArray.getJSONObject(i)
                    val id = aJson.optLong("id", 0)
                    val typeStr = aJson.optString("type", "")
                    val type = try {
                        ActivityType.valueOf(typeStr)
                    } catch (e: Exception) {
                        return Result.failure(Exception("Activity record at position $i has an invalid type: '$typeStr'."))
                    }

                    val friendId = aJson.optLong("friendId", -1)
                    if (friendId == -1L || friendId !in friendIds) {
                        return Result.failure(Exception("Activity record at position $i refers to non-existent Friend $friendId."))
                    }

                    val transactionId = if (aJson.isNull("transactionId")) null else aJson.getLong("transactionId")
                    if (transactionId != null && transactionId !in transactionIds) {
                        return Result.failure(Exception("Activity record at position $i refers to non-existent Transaction $transactionId."))
                    }

                    val directionStr = if (aJson.isNull("direction")) null else aJson.getString("direction")
                    val direction = if (directionStr != null) {
                        try {
                            TransactionDirection.valueOf(directionStr)
                        } catch (e: Exception) {
                            return Result.failure(Exception("Activity record at position $i has an invalid direction: '$directionStr'."))
                        }
                    } else null

                    val reminderStageStr = if (aJson.isNull("reminderStage")) null else aJson.getString("reminderStage")
                    val reminderStage = if (reminderStageStr != null) {
                        try {
                            ReminderStage.valueOf(reminderStageStr)
                        } catch (e: Exception) {
                            return Result.failure(Exception("Activity record at position $i has an invalid reminderStage: '$reminderStageStr'."))
                        }
                    } else null

                    val amountVal = if (aJson.isNull("amount")) null else aJson.getDouble("amount")
                    if (amountVal != null && (amountVal < 0.0 || amountVal.isNaN() || amountVal.isInfinite())) {
                        return Result.failure(Exception("Activity record at position $i has an invalid amount: $amountVal."))
                    }

                    val note = if (aJson.isNull("note")) null else aJson.getString("note")
                    val createdAt = aJson.optLong("createdAt", System.currentTimeMillis())

                    activitiesList.add(
                        ActivityEntity(
                            id = id,
                            type = type,
                            friendId = friendId,
                            transactionId = transactionId,
                            amount = amountVal,
                            direction = direction,
                            note = note,
                            reminderStage = reminderStage,
                            createdAt = createdAt
                        )
                    )
                }
            }

            // 6. Atomic Database Replacement & Preference Setup inside database.withTransaction
            repository.restoreValidatedData(
                friends = friendsList,
                transactions = transactionsList,
                activities = activitiesList,
                userName = userName,
                hasCompletedOnboarding = hasCompletedOnboarding,
                remindersEnabled = remindersEnabled,
                userPreferences = userPreferences
            )

            Result.success(Unit)
        } catch (e: JSONException) {
            Result.failure(Exception("Malformed JSON backup file: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
