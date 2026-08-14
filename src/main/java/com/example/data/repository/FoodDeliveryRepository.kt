package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.CartItemEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.OrderEntity
import com.example.data.local.SavedAddressEntity
import com.example.data.model.OrderStatus
import com.example.data.model.Restaurant
import com.example.data.sample.MockRestaurantData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull

class FoodDeliveryRepository(private val db: AppDatabase) {

    // Dynamic list of restaurants (updated via Live GPS / Google Maps Places)
    private val _restaurants = MutableStateFlow<List<Restaurant>>(MockRestaurantData.SAMPLE_RESTAURANTS)
    val restaurantsFlow: Flow<List<Restaurant>> = _restaurants.asStateFlow()

    fun getRestaurants(): List<Restaurant> = _restaurants.value

    fun updateRestaurants(list: List<Restaurant>) {
        if (list.isNotEmpty()) {
            _restaurants.value = list
        }
    }

    fun getRestaurantById(id: String): Restaurant? {
        return _restaurants.value.find { it.id == id } ?: MockRestaurantData.SAMPLE_RESTAURANTS.find { it.id == id }
    }

    // Orders
    val allOrders: Flow<List<OrderEntity>> = db.orderDao().getAllOrders()
    val activeOrder: Flow<OrderEntity?> = db.orderDao().getActiveOrder()

    fun getOrderById(orderId: Long): Flow<OrderEntity?> = db.orderDao().getOrderById(orderId)

    suspend fun getOrderByIdDirect(orderId: Long): OrderEntity? = db.orderDao().getOrderByIdDirect(orderId)

    suspend fun createOrder(order: OrderEntity): Long {
        return db.orderDao().insertOrder(order)
    }

    suspend fun updateOrderStatusAndLocation(
        orderId: Long,
        status: OrderStatus,
        etaMinutes: Int,
        progress: Float,
        lat: Double,
        lng: Double
    ) {
        db.orderDao().updateOrderStatusAndLocation(orderId, status, etaMinutes, progress, lat, lng)
    }

    suspend fun cancelOrder(orderId: Long) {
        val order = db.orderDao().getOrderByIdDirect(orderId)
        if (order != null) {
            db.orderDao().updateOrder(order.copy(status = OrderStatus.CANCELLED))
        }
    }

    // Cart
    val cartItems: Flow<List<CartItemEntity>> = db.cartDao().getCartItems()

    suspend fun addToCart(item: CartItemEntity) {
        val currentItems = db.cartDao().getCartItems().firstOrNull() ?: emptyList()
        val existing = currentItems.find {
            it.foodItemId == item.foodItemId &&
            it.selectedOptions == item.selectedOptions &&
            it.restaurantId == item.restaurantId
        }
        if (existing != null) {
            db.cartDao().updateItem(existing.copy(quantity = existing.quantity + item.quantity))
        } else {
            db.cartDao().insertOrUpdateItem(item)
        }
    }

    suspend fun updateCartItemQuantity(itemId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            db.cartDao().deleteItemById(itemId)
        } else {
            val currentItems = db.cartDao().getCartItems().firstOrNull() ?: emptyList()
            val item = currentItems.find { it.id == itemId }
            if (item != null) {
                db.cartDao().updateItem(item.copy(quantity = newQuantity))
            }
        }
    }

    suspend fun clearCart() = db.cartDao().clearCart()

    // Addresses
    val savedAddresses: Flow<List<SavedAddressEntity>> = db.addressDao().getAllAddresses()
    val defaultAddress: Flow<SavedAddressEntity?> = db.addressDao().getDefaultAddress()

    suspend fun addAddress(address: SavedAddressEntity) {
        if (address.isDefault) {
            db.addressDao().resetAllDefaults()
        }
        db.addressDao().insertAddress(address)
    }

    suspend fun setDefaultAddress(addressId: Long) {
        db.addressDao().resetAllDefaults()
        db.addressDao().setDefaultAddress(addressId)
    }

    suspend fun deleteAddress(address: SavedAddressEntity) {
        db.addressDao().deleteAddress(address)
    }

    suspend fun updateDefaultAddressFromGps(label: String, addressLine: String, lat: Double, lng: Double) {
        val defaultAddr = db.addressDao().getDefaultAddress().firstOrNull()
        if (defaultAddr != null) {
            db.addressDao().updateAddress(
                defaultAddr.copy(
                    label = label,
                    addressLine = addressLine,
                    lat = lat,
                    lng = lng
                )
            )
        } else {
            db.addressDao().insertAddress(
                SavedAddressEntity(
                    label = label,
                    addressLine = addressLine,
                    deliveryInstructions = "Leave at front door",
                    isDefault = true,
                    lat = lat,
                    lng = lng
                )
            )
        }
    }

    // Seed default address if none exists
    suspend fun seedInitialDataIfEmpty() {
        val addresses = db.addressDao().getAllAddresses().firstOrNull() ?: emptyList()
        if (addresses.isEmpty()) {
            db.addressDao().insertAddress(
                SavedAddressEntity(
                    label = "Home",
                    addressLine = "Flat 402, Green Valley Apts, Indiranagar",
                    deliveryInstructions = "Ring buzzer #402, leave at front door",
                    isDefault = true,
                    lat = 12.9716,
                    lng = 77.5946
                )
            )
            db.addressDao().insertAddress(
                SavedAddressEntity(
                    label = "Work",
                    addressLine = "Level 5, Tech Park, Outer Ring Road",
                    deliveryInstructions = "Call upon arrival in reception",
                    isDefault = false,
                    lat = 12.9352,
                    lng = 77.6245
                )
            )
        }

        val notifications = db.notificationDao().getAllNotifications().firstOrNull() ?: emptyList()
        if (notifications.isEmpty()) {
            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "🎉 Welcome to BiteSprint!",
                    message = "Get flat 20% off your first delivery order with code BITESPRINT20.",
                    type = "PROMO"
                )
            )
        }
    }

    // Notifications
    val notifications: Flow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = db.notificationDao().getUnreadCount()

    suspend fun addNotification(title: String, message: String, orderId: Long? = null, type: String = "ORDER_STATUS") {
        db.notificationDao().insertNotification(
            NotificationEntity(
                title = title,
                message = message,
                orderId = orderId,
                type = type
            )
        )
    }

    suspend fun markNotificationAsRead(id: Long) = db.notificationDao().markAsRead(id)
    suspend fun markAllNotificationsAsRead() = db.notificationDao().markAllAsRead()
    suspend fun clearNotifications() = db.notificationDao().clearAll()
}
