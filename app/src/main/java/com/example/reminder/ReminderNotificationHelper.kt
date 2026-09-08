package com.example.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.ReminderStage
import com.example.ui.util.Formatters

object ReminderNotificationHelper {
    const val CHANNEL_ID = "repayment_reminders"
    const val CHANNEL_NAME = "Repayment reminders"
    const val CHANNEL_DESC = "Reminders for overdue money you lent."

    const val EXTRA_FRIEND_ID = "extra_friend_id"
    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (!notificationManagerCompat.areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun sendReminderNotification(
        context: Context,
        friendName: String,
        amount: Double,
        stage: ReminderStage,
        transactionId: Long,
        friendId: Long
    ): Boolean {
        if (!canPostNotifications(context)) {
            return false
        }

        ensureNotificationChannel(context)

        val title = stage.formatTitle(friendName)
        val formattedAmount = Formatters.formatCurrency(amount)
        val body = stage.formatBody(formattedAmount)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FRIEND_ID, friendId)
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            generateRequestCode(transactionId, stage),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationId = generateNotificationId(transactionId, stage)

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun generateNotificationId(transactionId: Long, stage: ReminderStage): Int {
        val hash = (transactionId * 31 + stage.ordinal).toInt()
        return (hash and 0x7FFFFFFF).coerceAtLeast(1)
    }

    fun cancelAllNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (e: Exception) {
            // Ignore failure on cancel
        }
    }

    private fun generateRequestCode(transactionId: Long, stage: ReminderStage): Int {
        val hash = (transactionId * 37 + stage.ordinal + 1000).toInt()
        return (hash and 0x7FFFFFFF).coerceAtLeast(1)
    }
}
