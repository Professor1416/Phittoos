package com.professor1416.phittoos.export

import android.content.Context
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.ActivityEntity
import com.professor1416.phittoos.data.model.ActivityType
import com.professor1416.phittoos.data.model.Friend
import com.professor1416.phittoos.data.model.ReminderStage
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionEntity
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.PhittoosRepository
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

    data class BackupSummary(
        val profileName: String,
        val createdAt: Long,
        val friendCount: Int,
        val transactionCount: Int
    )

    fun validateBackup(jsonString: String): Result<BackupSummary> {
        val trimmed = jsonString.trim()
        if (!trimmed.startsWith("{")) {
            if (trimmed.contains(",") || trimmed.contains("\n")) {
                return Result.failure(Exception("CSV files can’t be restored. CSV export is for viewing or sharing your records. Select a Phittoos backup file instead."))
            } else {
                return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
            }
        }

        try {
            val root = JSONObject(jsonString)

            // 1. Metadata Section
            if (!root.has("metadata")) {
                return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
            }
            val metadata = root.getJSONObject("metadata")
            val backupVersion = metadata.optInt("backupVersion", -1)
            if (backupVersion == -1) {
                return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
            }
            if (backupVersion > BACKUP_VERSION) {
                return Result.failure(Exception("This backup was created by a newer version of Phittoos and can’t be restored with this version."))
            }

            // 2. Preferences Section
            if (!root.has("preferences")) {
                return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
            }
            val prefsJson = root.getJSONObject("preferences")
            val userName = prefsJson.optString("userName", "")
            if (userName.isBlank()) {
                return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
            }

            // 3. Friends Section
            if (!root.has("friends")) {
                return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
            }
            val friendsArray = root.getJSONArray("friends")
            val friendIds = mutableSetOf<Long>()
            for (i in 0 until friendsArray.length()) {
                val fJson = friendsArray.getJSONObject(i)
                val id = fJson.optLong("id", -1L)
                if (id <= 0L) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }
                if (friendIds.contains(id)) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }
                friendIds.add(id)

                val name = fJson.optString("name", "")
                if (name.isBlank()) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }
            }

            // 4. Transactions Section
            if (!root.has("transactions")) {
                return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
            }
            val txsArray = root.getJSONArray("transactions")
            val transactionIds = mutableSetOf<Long>()
            for (i in 0 until txsArray.length()) {
                val tJson = txsArray.getJSONObject(i)
                val id = tJson.optLong("id", -1L)
                if (id <= 0L) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }
                if (transactionIds.contains(id)) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }
                transactionIds.add(id)

                val friendId = tJson.optLong("friendId", -1L)
                if (friendId <= 0L || friendId !in friendIds) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }

                val amount = tJson.optDouble("amount", -1.0)
                if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }

                val directionStr = tJson.optString("direction", "")
                try {
                    TransactionDirection.valueOf(directionStr)
                } catch (e: Exception) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }

                val statusStr = tJson.optString("status", "")
                try {
                    TransactionStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }

                val paidAmount = if (tJson.isNull("paidAmount")) null else tJson.getDouble("paidAmount")
                if (paidAmount != null) {
                    if (paidAmount < 0.0 || paidAmount.isNaN() || paidAmount.isInfinite()) {
                        return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                    }
                    if (paidAmount > amount) {
                        return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                    }
                }
            }

            // 5. Activities Section
            if (!root.has("activities")) {
                return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
            }
            val actsArray = root.getJSONArray("activities")
            val activityIds = mutableSetOf<Long>()
            for (i in 0 until actsArray.length()) {
                val aJson = actsArray.getJSONObject(i)
                val id = aJson.optLong("id", -1L)
                if (id <= 0L) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }
                if (activityIds.contains(id)) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }
                activityIds.add(id)

                val typeStr = aJson.optString("type", "")
                try {
                    ActivityType.valueOf(typeStr)
                } catch (e: Exception) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }

                val friendId = aJson.optLong("friendId", -1L)
                if (friendId <= 0L || friendId !in friendIds) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }

                val transactionId = if (aJson.isNull("transactionId")) null else aJson.getLong("transactionId")
                if (transactionId != null && transactionId !in transactionIds) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }

                val directionStr = if (aJson.isNull("direction")) null else aJson.getString("direction")
                if (directionStr != null) {
                    try {
                        TransactionDirection.valueOf(directionStr)
                    } catch (e: Exception) {
                        return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                    }
                }

                val reminderStageStr = if (aJson.isNull("reminderStage")) null else aJson.getString("reminderStage")
                if (reminderStageStr != null) {
                    try {
                        ReminderStage.valueOf(reminderStageStr)
                    } catch (e: Exception) {
                        return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                    }
                }

                val amountVal = if (aJson.isNull("amount")) null else aJson.getDouble("amount")
                if (amountVal != null && (amountVal <= 0.0 || amountVal.isNaN() || amountVal.isInfinite())) {
                    return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
                }
            }

            return Result.success(
                BackupSummary(
                    profileName = userName,
                    createdAt = metadata.optLong("createdAt", 0L),
                    friendCount = friendsArray.length(),
                    transactionCount = txsArray.length()
                )
            )

        } catch (e: Exception) {
            return Result.failure(Exception("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’."))
        }
    }

    suspend fun restoreBackup(
        jsonString: String,
        repository: PhittoosRepository,
        userPreferences: UserPreferences,
        database: AppDatabase
    ): Result<Unit> {
        val validation = validateBackup(jsonString)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        return try {
            val root = JSONObject(jsonString)

            // Preferences
            val prefsJson = root.getJSONObject("preferences")
            val userName = prefsJson.getString("userName")
            val hasCompletedOnboarding = prefsJson.optBoolean("hasCompletedOnboarding", false)
            val remindersEnabled = prefsJson.optBoolean("remindersEnabled", true)

            // Friends
            val friendsArray = root.getJSONArray("friends")
            val friendsList = ArrayList<Friend>()
            for (i in 0 until friendsArray.length()) {
                val fJson = friendsArray.getJSONObject(i)
                val id = fJson.getLong("id")
                val name = fJson.getString("name")
                val contactInfo = if (fJson.isNull("contactInfo")) null else fJson.getString("contactInfo")
                val reliabilityTier = fJson.optString("reliabilityTier", "NEW")
                friendsList.add(
                    Friend(
                        id = id,
                        name = name,
                        contactInfo = contactInfo,
                        reliabilityTier = reliabilityTier
                    )
                )
            }

            // Transactions
            val txsArray = root.getJSONArray("transactions")
            val transactionsList = ArrayList<TransactionEntity>()
            for (i in 0 until txsArray.length()) {
                val tJson = txsArray.getJSONObject(i)
                val id = tJson.getLong("id")
                val friendId = tJson.getLong("friendId")
                val amount = tJson.getDouble("amount")
                val direction = TransactionDirection.valueOf(tJson.getString("direction"))
                val note = if (tJson.isNull("note")) null else tJson.getString("note")
                val createdDate = tJson.optLong("createdDate", System.currentTimeMillis())
                val dueDate = if (tJson.isNull("dueDate")) null else tJson.getLong("dueDate")
                val status = TransactionStatus.valueOf(tJson.getString("status"))
                val paidAmount = if (tJson.isNull("paidAmount")) null else tJson.getDouble("paidAmount")
                val settledAt = if (tJson.isNull("settledAt")) null else tJson.getLong("settledAt")

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

            // Activities
            val activitiesList = ArrayList<ActivityEntity>()
            val actsArray = root.getJSONArray("activities")
            for (i in 0 until actsArray.length()) {
                val aJson = actsArray.getJSONObject(i)
                val id = aJson.optLong("id", 0)
                val type = ActivityType.valueOf(aJson.getString("type"))
                val friendId = aJson.getLong("friendId")
                val transactionId = if (aJson.isNull("transactionId")) null else aJson.getLong("transactionId")
                val amountVal = if (aJson.isNull("amount")) null else aJson.getDouble("amount")
                val direction = if (aJson.isNull("direction")) null else TransactionDirection.valueOf(aJson.getString("direction"))
                val note = if (aJson.isNull("note")) null else aJson.getString("note")
                val reminderStage = if (aJson.isNull("reminderStage")) null else ReminderStage.valueOf(aJson.getString("reminderStage"))
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

            // Atomic replacement inside a Room transaction
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
