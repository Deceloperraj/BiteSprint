package com.example.notification

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
import com.example.R
import com.example.data.model.OrderStatus

class DeliveryNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "bitesprint_order_tracking"
        const val CHANNEL_NAME = "Food Delivery Real-Time Updates"
        const val CHANNEL_DESC = "Real-time push notifications for order progress and courier arrival"
        const val NOTIFICATION_ID_BASE = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendOrderStatusNotification(
        orderId: Long,
        orderNumber: String,
        restaurantName: String,
        status: OrderStatus,
        driverName: String,
        etaMinutes: Int
    ) {
        val (title, content) = when (status) {
            OrderStatus.PLACED -> Pair(
                "🛵 Order Placed ($orderNumber)",
                "Sent to $restaurantName. Preparing to confirm your order!"
            )
            OrderStatus.CONFIRMED -> Pair(
                "✅ Order Confirmed!",
                "$restaurantName accepted your order and will start cooking shortly."
            )
            OrderStatus.PREPARING -> Pair(
                "🍳 Kitchen is Cooking!",
                "Chef is preparing your meal. Estimated delivery in ~$etaMinutes mins."
            )
            OrderStatus.OUT_FOR_DELIVERY -> Pair(
                "🚀 Courier On The Way!",
                "$driverName picked up your food from $restaurantName. ETA ~$etaMinutes mins."
            )
            OrderStatus.DELIVERED -> Pair(
                "🎉 Order Delivered! Bon Appétit!",
                "Your meal from $restaurantName has arrived at your door. Enjoy!"
            )
            OrderStatus.CANCELLED -> Pair(
                "❌ Order Cancelled",
                "Order $orderNumber has been cancelled."
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ORDER_ID", orderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            orderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        if (status == OrderStatus.OUT_FOR_DELIVERY || status == OrderStatus.DELIVERED) {
            builder.setVibrate(longArrayOf(0, 250, 100, 250))
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    NotificationManagerCompat.from(context).notify(
                        (NOTIFICATION_ID_BASE + (orderId % 1000)).toInt(),
                        builder.build()
                    )
                }
            } else {
                NotificationManagerCompat.from(context).notify(
                    (NOTIFICATION_ID_BASE + (orderId % 1000)).toInt(),
                    builder.build()
                )
            }
        } catch (e: Exception) {
            // Gracefully catch if notifications are disabled in emulator
        }
    }
}
