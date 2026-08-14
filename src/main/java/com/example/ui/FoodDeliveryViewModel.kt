package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CartItemEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.OrderEntity
import com.example.data.local.SavedAddressEntity
import com.example.data.location.LocationHelper
import com.example.data.location.UserLocationResult
import com.example.data.model.CustomizationItem
import com.example.data.model.FoodItem
import com.example.data.model.OrderStatus
import com.example.data.model.Restaurant
import com.example.data.repository.FoodDeliveryRepository
import com.example.data.repository.NearbyRestaurantRepository
import com.example.data.sample.MockRestaurantData
import com.example.notification.DeliveryNotificationHelper
import com.example.tracking.TrackingEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

enum class AppScreen {
    HOME,
    RESTAURANT_DETAIL,
    CART_CHECKOUT,
    TRACKING,
    ORDERS,
    NOTIFICATIONS,
    ADDRESSES
}

data class InAppBanner(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val type: String = "STATUS"
)

class FoodDeliveryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FoodDeliveryRepository(db)
    private val notificationHelper = DeliveryNotificationHelper(application)
    private val locationHelper = LocationHelper(application)
    private val nearbyRestaurantRepository = NearbyRestaurantRepository()

    // Current Screen & Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedRestaurantId = MutableStateFlow<String?>(null)
    val selectedRestaurantId: StateFlow<String?> = _selectedRestaurantId.asStateFlow()

    private val _selectedFoodItem = MutableStateFlow<FoodItem?>(null)
    val selectedFoodItem: StateFlow<FoodItem?> = _selectedFoodItem.asStateFlow()

    // Tracking Order Selection
    private val _trackingOrderId = MutableStateFlow<Long?>(null)
    val trackingOrderId: StateFlow<Long?> = _trackingOrderId.asStateFlow()

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCuisineFilter = MutableStateFlow("All")
    val selectedCuisineFilter: StateFlow<String> = _selectedCuisineFilter.asStateFlow()

    // GPS & Location Status
    private val _isLocationLoading = MutableStateFlow(false)
    val isLocationLoading: StateFlow<Boolean> = _isLocationLoading.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(locationHelper.hasLocationPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _currentGpsAddress = MutableStateFlow<UserLocationResult?>(null)
    val currentGpsAddress: StateFlow<UserLocationResult?> = _currentGpsAddress.asStateFlow()

    // Promo Code & Payment
    private val _appliedPromoCode = MutableStateFlow<String?>(null)
    val appliedPromoCode: StateFlow<String?> = _appliedPromoCode.asStateFlow()

    private val _promoDiscountPercent = MutableStateFlow(0)
    val promoDiscountPercent: StateFlow<Int> = _promoDiscountPercent.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow("UPI (GPay / PhonePe)")
    val selectedPaymentMethod: StateFlow<String> = _selectedPaymentMethod.asStateFlow()

    // In-App Notification Banner Queue
    private val _activeBanner = MutableStateFlow<InAppBanner?>(null)
    val activeBanner: StateFlow<InAppBanner?> = _activeBanner.asStateFlow()

    // Simulation Speed Multiplier (1x, 2x, 5x)
    private val _simulationSpeedMultiplier = MutableStateFlow(1f)
    val simulationSpeedMultiplier: StateFlow<Float> = _simulationSpeedMultiplier.asStateFlow()

    private val _isSimulationPaused = MutableStateFlow(false)
    val isSimulationPaused: StateFlow<Boolean> = _isSimulationPaused.asStateFlow()

    // Simulation Job
    private var trackingSimulationJob: Job? = null

    // Room & Dynamic Data Flows
    val allRestaurants = repository.restaurantsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.getRestaurants()
    )
    val allOrders = repository.allOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeOrder = repository.activeOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val cartItems = repository.cartItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savedAddresses = repository.savedAddresses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val defaultAddress = repository.defaultAddress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val notifications = repository.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unreadNotificationsCount = repository.unreadNotificationsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Calculated Cart Totals
    val cartSubtotal = cartItems.combine(cartItems) { items, _ ->
        items.sumOf { it.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            if (locationHelper.hasLocationPermission()) {
                requestLocationAndFetchNearby()
            }
        }
    }

    fun onLocationPermissionGranted() {
        onLocationPermissionResult(true)
    }

    fun onLocationPermissionResult(isGranted: Boolean) {
        _hasLocationPermission.value = isGranted
        if (isGranted) {
            requestLocationAndFetchNearby()
        } else {
            showInAppBanner("Location Access", "Using default area restaurants.")
        }
    }

    fun requestLocationAndFetchNearby() {
        viewModelScope.launch {
            _isLocationLoading.value = true
            try {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    val geoResult = locationHelper.reverseGeocode(location.latitude, location.longitude)
                    _currentGpsAddress.value = geoResult

                    // Update default delivery address in Room
                    repository.updateDefaultAddressFromGps(
                        label = geoResult.addressLabel,
                        addressLine = geoResult.fullAddress,
                        lat = geoResult.latitude,
                        lng = geoResult.longitude
                    )

                    // Fetch real nearby restaurants via GPS & Google / OSM Places
                    val nearbyList = nearbyRestaurantRepository.fetchNearbyRestaurants(
                        userLat = location.latitude,
                        userLng = location.longitude,
                        city = geoResult.city
                    )

                    if (nearbyList.isNotEmpty()) {
                        repository.updateRestaurants(nearbyList)
                        showInAppBanner("📍 Location Updated", "Found ${nearbyList.size} restaurants near ${geoResult.addressLabel}")
                    }
                }
            } catch (e: Exception) {
                // Fallback gracefully
            } finally {
                _isLocationLoading.value = false
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectRestaurant(restaurantId: String) {
        _selectedRestaurantId.value = restaurantId
        _currentScreen.value = AppScreen.RESTAURANT_DETAIL
    }

    fun openFoodItemDetails(foodItem: FoodItem) {
        _selectedFoodItem.value = foodItem
    }

    fun closeFoodItemDetails() {
        _selectedFoodItem.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCuisineFilter(cuisine: String) {
        _selectedCuisineFilter.value = cuisine
    }

    fun setPaymentMethod(method: String) {
        _selectedPaymentMethod.value = method
    }

    fun applyPromoCode(code: String): Boolean {
        val clean = code.trim().uppercase(Locale.ROOT)
        return when (clean) {
            "BITESPRINT20", "TASTY20", "WELCOME50" -> {
                _appliedPromoCode.value = clean
                _promoDiscountPercent.value = 20
                showInAppBanner("Promo Applied! 🏷️", "20% discount added to your cart.")
                true
            }
            "FREESHIP", "FREEDEL" -> {
                _appliedPromoCode.value = clean
                _promoDiscountPercent.value = 15
                showInAppBanner("Free Delivery Applied! 🚀", "Delivery discount added.")
                true
            }
            "FEAST100", "SAVE10" -> {
                _appliedPromoCode.value = clean
                _promoDiscountPercent.value = 10
                showInAppBanner("Discount Applied! ✨", "Enjoy 10% savings on your feast.")
                true
            }
            else -> false
        }
    }

    fun removePromoCode() {
        _appliedPromoCode.value = null
        _promoDiscountPercent.value = 0
    }

    fun addToCart(
        foodItem: FoodItem,
        restaurant: Restaurant,
        quantity: Int,
        customizations: List<CustomizationItem> = emptyList()
    ) {
        viewModelScope.launch {
            val extraPrice = customizations.sumOf { it.extraPrice }
            val unitPrice = foodItem.price + extraPrice
            val customNotes = customizations.joinToString(", ") { it.name }

            repository.addToCart(
                CartItemEntity(
                    restaurantId = restaurant.id,
                    restaurantName = restaurant.name,
                    foodItemId = foodItem.id,
                    name = foodItem.name,
                    price = unitPrice,
                    quantity = quantity,
                    selectedOptions = customNotes
                )
            )
            showInAppBanner("Added to Cart 🛒", "${foodItem.name} (x$quantity) added.")
            _selectedFoodItem.value = null
        }
    }

    fun updateCartQuantity(itemId: Long, quantity: Int) {
        viewModelScope.launch {
            repository.updateCartItemQuantity(itemId, quantity)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun checkoutAndPlaceOrder(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val items = cartItems.value
            if (items.isEmpty()) return@launch

            val restId = items.first().restaurantId
            val restaurant = repository.getRestaurantById(restId)
            val subtotal = items.sumOf { it.price * it.quantity }
            val deliveryFee = restaurant?.deliveryFee ?: 35.0
            val discount = (subtotal * (_promoDiscountPercent.value / 100.0))
            val tax = subtotal * 0.05 // 5% GST
            val total = (subtotal + deliveryFee + tax - discount).coerceAtLeast(0.0)

            val currentGps = _currentGpsAddress.value
            val address = defaultAddress.value?.addressLine ?: currentGps?.fullAddress ?: "Indiranagar, 100ft Road, Bengaluru"
            val addressLabel = defaultAddress.value?.label ?: currentGps?.addressLabel ?: "Current Location"
            val customerLat = defaultAddress.value?.lat ?: currentGps?.latitude ?: 12.9716
            val customerLng = defaultAddress.value?.lng ?: currentGps?.longitude ?: 77.5946

            val restLat = restaurant?.lat ?: (customerLat + 0.012)
            val restLng = restaurant?.lng ?: (customerLng + 0.008)

            val randomDigits = Random.nextInt(1000, 9999)
            val orderNum = "#BS-$randomDigits"
            val summary = items.joinToString(", ") { "${it.quantity}x ${it.name}" }

            val orderEntity = OrderEntity(
                orderNumber = orderNum,
                restaurantId = restId,
                restaurantName = restaurant?.name ?: items.first().restaurantName,
                itemsSummary = summary,
                totalAmount = total,
                status = OrderStatus.PLACED,
                createdAt = System.currentTimeMillis(),
                etaMinutes = restaurant?.deliveryTimeMinutes ?: 25,
                driverName = listOf("Rahul Sharma", "Amit Patel", "Deepak Verma", "Suresh Kumar").random(),
                driverPhone = "+91 98${Random.nextInt(100, 999)} ${Random.nextInt(10000, 99999)}",
                driverRating = (4.8 + Random.nextDouble(0.0, 0.2)),
                driverVehicle = "Hero Electric Optima (Blue)",
                deliveryAddress = address,
                addressLabel = addressLabel,
                restaurantLat = restLat,
                restaurantLng = restLng,
                customerLat = customerLat,
                customerLng = customerLng,
                currentDriverLat = restLat,
                currentDriverLng = restLng,
                simulationProgress = 0.02f
            )

            val newOrderId = repository.createOrder(orderEntity)
            repository.clearCart()
            removePromoCode()

            // Push notification for order placed
            notificationHelper.sendOrderStatusNotification(
                orderId = newOrderId,
                orderNumber = orderNum,
                restaurantName = orderEntity.restaurantName,
                status = OrderStatus.PLACED,
                driverName = orderEntity.driverName,
                etaMinutes = orderEntity.etaMinutes
            )
            repository.addNotification(
                title = "🛵 Order Placed ($orderNum)",
                message = "Order sent to ${orderEntity.restaurantName}. Estimated ETA ~${orderEntity.etaMinutes} mins.",
                orderId = newOrderId
            )

            _trackingOrderId.value = newOrderId
            _currentScreen.value = AppScreen.TRACKING

            // Start live simulation loop
            startRealTimeTrackingSimulation(newOrderId)
            onSuccess(newOrderId)
        }
    }

    /**
     * Quick Demo Order Trigger - Instantly generates a live tracking simulation with Indian Rupee pricing
     */
    fun placeQuickDemoOrder() {
        val sampleRest = repository.getRestaurants().firstOrNull() ?: MockRestaurantData.SAMPLE_RESTAURANTS.first()
        viewModelScope.launch {
            val orderNum = "#BS-${Random.nextInt(1000, 9999)}"
            val currentGps = _currentGpsAddress.value
            val custLat = defaultAddress.value?.lat ?: currentGps?.latitude ?: 12.9716
            val custLng = defaultAddress.value?.lng ?: currentGps?.longitude ?: 77.5946
            val addrLine = defaultAddress.value?.addressLine ?: currentGps?.fullAddress ?: "Indiranagar, Bengaluru"

            val demoOrder = OrderEntity(
                orderNumber = orderNum,
                restaurantId = sampleRest.id,
                restaurantName = sampleRest.name,
                itemsSummary = "1x Paneer Butter Masala, 2x Garlic Butter Naan",
                totalAmount = 379.0,
                status = OrderStatus.PLACED,
                createdAt = System.currentTimeMillis(),
                etaMinutes = sampleRest.deliveryTimeMinutes,
                driverName = "Rahul Sharma",
                driverPhone = "+91 98765 43210",
                driverRating = 4.95,
                driverVehicle = "Ather 450X (Space Grey)",
                deliveryAddress = addrLine,
                addressLabel = "Live GPS Location",
                restaurantLat = sampleRest.lat,
                restaurantLng = sampleRest.lng,
                customerLat = custLat,
                customerLng = custLng,
                currentDriverLat = sampleRest.lat,
                currentDriverLng = sampleRest.lng,
                simulationProgress = 0.02f
            )

            val orderId = repository.createOrder(demoOrder)
            notificationHelper.sendOrderStatusNotification(
                orderId = orderId,
                orderNumber = orderNum,
                restaurantName = sampleRest.name,
                status = OrderStatus.PLACED,
                driverName = demoOrder.driverName,
                etaMinutes = demoOrder.etaMinutes
            )
            repository.addNotification(
                title = "🛵 Live Order Placed ($orderNum)",
                message = "Live tracking simulation started for ${sampleRest.name}!",
                orderId = orderId
            )
            showInAppBanner("Order Active!", "Real-time courier map & push notifications running.")
            _trackingOrderId.value = orderId
            _currentScreen.value = AppScreen.TRACKING
            startRealTimeTrackingSimulation(orderId)
        }
    }

    fun viewOrderTracking(orderId: Long) {
        _trackingOrderId.value = orderId
        _currentScreen.value = AppScreen.TRACKING
        startRealTimeTrackingSimulation(orderId)
    }

    fun setSimulationSpeed(multiplier: Float) {
        _simulationSpeedMultiplier.value = multiplier
    }

    fun toggleSimulationPause() {
        _isSimulationPaused.value = !_isSimulationPaused.value
    }

    /**
     * Real-Time Driver & Order Tracking Simulation Engine:
     * Advances progress smoothly through the 5 stages.
     */
    fun startRealTimeTrackingSimulation(orderId: Long) {
        trackingSimulationJob?.cancel()
        trackingSimulationJob = viewModelScope.launch {
            var currentOrder = repository.getOrderByIdDirect(orderId) ?: return@launch
            if (currentOrder.status == OrderStatus.DELIVERED || currentOrder.status == OrderStatus.CANCELLED) {
                return@launch
            }

            var progress = currentOrder.simulationProgress.coerceAtLeast(0.01f)
            val waypoints = TrackingEngine.generateRouteWaypoints(
                currentOrder.restaurantLat,
                currentOrder.restaurantLng,
                currentOrder.customerLat,
                currentOrder.customerLng
            )

            var lastReportedStatus = currentOrder.status

            while (progress < 1.0f) {
                while (_isSimulationPaused.value) {
                    delay(500)
                }

                val speed = _simulationSpeedMultiplier.value
                val stepIncrement = 0.025f * speed
                progress = (progress + stepIncrement).coerceAtMost(1.0f)

                val newStatus = TrackingEngine.progressToStatus(progress)
                val (curLat, curLng) = TrackingEngine.interpolateLocation(waypoints, progress)
                val remainingEta = ((1.0f - progress) * currentOrder.etaMinutes.coerceAtLeast(15)).toInt().coerceAtLeast(1)

                repository.updateOrderStatusAndLocation(
                    orderId = orderId,
                    status = newStatus,
                    etaMinutes = if (newStatus == OrderStatus.DELIVERED) 0 else remainingEta,
                    progress = progress,
                    lat = curLat,
                    lng = curLng
                )

                // Check for stage transitions and fire push notifications
                if (newStatus != lastReportedStatus) {
                    lastReportedStatus = newStatus
                    val updated = repository.getOrderByIdDirect(orderId) ?: currentOrder

                    notificationHelper.sendOrderStatusNotification(
                        orderId = orderId,
                        orderNumber = updated.orderNumber,
                        restaurantName = updated.restaurantName,
                        status = newStatus,
                        driverName = updated.driverName,
                        etaMinutes = if (newStatus == OrderStatus.DELIVERED) 0 else remainingEta
                    )

                    repository.addNotification(
                        title = when (newStatus) {
                            OrderStatus.CONFIRMED -> "✅ Order Confirmed"
                            OrderStatus.PREPARING -> "🍳 Kitchen is Cooking"
                            OrderStatus.OUT_FOR_DELIVERY -> "🚀 Courier On The Way"
                            OrderStatus.DELIVERED -> "🎉 Order Delivered!"
                            else -> "Update"
                        },
                        message = when (newStatus) {
                            OrderStatus.CONFIRMED -> "${updated.restaurantName} confirmed your order."
                            OrderStatus.PREPARING -> "Chef is crafting and packaging your meal."
                            OrderStatus.OUT_FOR_DELIVERY -> "${updated.driverName} is on the way to your address."
                            OrderStatus.DELIVERED -> "Delivered at ${updated.deliveryAddress}. Enjoy your food!"
                            else -> ""
                        },
                        orderId = orderId
                    )

                    showInAppBanner(
                        title = newStatus.displayName,
                        message = "${updated.restaurantName}: ${newStatus.description}"
                    )
                }

                // Smooth update interval
                delay((1000 / speed).toLong().coerceAtLeast(200))
            }
        }
    }

    fun fastForwardTracking(orderId: Long) {
        viewModelScope.launch {
            val order = repository.getOrderByIdDirect(orderId) ?: return@launch
            val nextProgress = when (order.status) {
                OrderStatus.PLACED -> 0.26f
                OrderStatus.CONFIRMED -> 0.46f
                OrderStatus.PREPARING -> 0.70f
                OrderStatus.OUT_FOR_DELIVERY -> 1.0f
                else -> 1.0f
            }
            val waypoints = TrackingEngine.generateRouteWaypoints(
                order.restaurantLat,
                order.restaurantLng,
                order.customerLat,
                order.customerLng
            )
            val (curLat, curLng) = TrackingEngine.interpolateLocation(waypoints, nextProgress)
            val newStatus = TrackingEngine.progressToStatus(nextProgress)
            val remainingEta = ((1.0f - nextProgress) * order.etaMinutes).toInt().coerceAtLeast(if (newStatus == OrderStatus.DELIVERED) 0 else 1)

            repository.updateOrderStatusAndLocation(
                orderId = orderId,
                status = newStatus,
                etaMinutes = remainingEta,
                progress = nextProgress,
                lat = curLat,
                lng = curLng
            )

            notificationHelper.sendOrderStatusNotification(
                orderId = orderId,
                orderNumber = order.orderNumber,
                restaurantName = order.restaurantName,
                status = newStatus,
                driverName = order.driverName,
                etaMinutes = remainingEta
            )

            repository.addNotification(
                title = "Status: ${newStatus.displayName}",
                message = "${order.restaurantName} - ${newStatus.description}",
                orderId = orderId
            )

            showInAppBanner("Fast-Forwarded", "${newStatus.displayName}: ${newStatus.description}")
        }
    }

    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            trackingSimulationJob?.cancel()
            repository.cancelOrder(orderId)
            val order = repository.getOrderByIdDirect(orderId)
            if (order != null) {
                notificationHelper.sendOrderStatusNotification(
                    orderId = orderId,
                    orderNumber = order.orderNumber,
                    restaurantName = order.restaurantName,
                    status = OrderStatus.CANCELLED,
                    driverName = order.driverName,
                    etaMinutes = 0
                )
                repository.addNotification(
                    title = "Order Cancelled",
                    message = "Order ${order.orderNumber} was cancelled.",
                    orderId = orderId
                )
                showInAppBanner("Order Cancelled", "Order ${order.orderNumber} has been cancelled.")
            }
        }
    }

    // Address Actions
    fun setDefaultAddress(addressId: Long) {
        viewModelScope.launch {
            repository.setDefaultAddress(addressId)
            showInAppBanner("Delivery Address Updated", "Selected address set as default.")
        }
    }

    fun addNewAddress(label: String, addressLine: String, instructions: String) {
        viewModelScope.launch {
            val currentLat = _currentGpsAddress.value?.latitude ?: 12.9716
            val currentLng = _currentGpsAddress.value?.longitude ?: 77.5946
            val newAddress = SavedAddressEntity(
                label = label,
                addressLine = addressLine,
                deliveryInstructions = instructions,
                isDefault = true,
                lat = currentLat + Random.nextDouble(-0.005, 0.005),
                lng = currentLng + Random.nextDouble(-0.005, 0.005)
            )
            repository.addAddress(newAddress)
            showInAppBanner("New Address Saved", "$label: $addressLine")
        }
    }

    // Notification Actions
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            showInAppBanner("Notifications Cleared", "All notifications marked as read.")
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    fun showInAppBanner(title: String, message: String) {
        _activeBanner.value = InAppBanner(title = title, message = message)
        viewModelScope.launch {
            delay(4000)
            if (_activeBanner.value?.title == title) {
                _activeBanner.value = null
            }
        }
    }

    fun dismissBanner() {
        _activeBanner.value = null
    }
}
