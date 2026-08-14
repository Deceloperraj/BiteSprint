package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.OrderStatus

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val restaurantId: String,
    val restaurantName: String,
    val itemsSummary: String,
    val totalAmount: Double,
    val status: OrderStatus = OrderStatus.PLACED,
    val createdAt: Long = System.currentTimeMillis(),
    val etaMinutes: Int = 25,
    val driverName: String = "Alex Martinez",
    val driverPhone: String = "+1 (555) 234-8901",
    val driverRating: Double = 4.92,
    val driverVehicle: String = "E-Scooter Sprint Pro",
    val deliveryAddress: String = "742 Evergreen Terrace, Apt 4B",
    val addressLabel: String = "Home",
    val restaurantLat: Double = 37.7749,
    val restaurantLng: Double = -122.4194,
    val customerLat: Double = 37.7885,
    val customerLng: Double = -122.4072,
    val currentDriverLat: Double = 37.7749,
    val currentDriverLng: Double = -122.4194,
    val simulationProgress: Float = 0f // 0.0 to 1.0
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val restaurantId: String,
    val restaurantName: String,
    val foodItemId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val selectedOptions: String = ""
)

@Entity(tableName = "saved_addresses")
data class SavedAddressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String, // Home, Work, Gym, etc.
    val addressLine: String,
    val deliveryInstructions: String = "",
    val isDefault: Boolean = false,
    val lat: Double = 37.7885,
    val lng: Double = -122.4072
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val orderId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "ORDER_STATUS" // ORDER_STATUS, PROMO, SYSTEM
)
