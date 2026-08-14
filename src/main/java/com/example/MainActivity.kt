package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.model.OrderStatus
import com.example.ui.AppScreen
import com.example.ui.FoodDeliveryViewModel
import com.example.ui.components.AddressManagementBottomSheet
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.NotificationBanner
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveTrackingScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OrdersHistoryScreen
import com.example.ui.screens.RestaurantDetailScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FoodDeliveryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle intent order tracking navigation
        val orderIdFromIntent = intent.getLongExtra("EXTRA_ORDER_ID", -1L)
        if (orderIdFromIntent != -1L) {
            viewModel.viewOrderTracking(orderIdFromIntent)
        }

        setContent {
            MyApplicationTheme {
                FoodDeliveryApp(
                    viewModel = viewModel,
                    onRequestNotificationPermission = { launcher ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FoodDeliveryApp(
    viewModel: FoodDeliveryViewModel,
    onRequestNotificationPermission: (androidx.activity.result.ActivityResultLauncher<String>) -> Unit
) {
    // Request notification permission on first launch for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Request GPS Location Permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.onLocationPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        onRequestNotificationPermission(notificationPermissionLauncher)
        // Prompt for GPS location on launch
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // State Collection
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedRestaurantId by viewModel.selectedRestaurantId.collectAsState()
    val selectedFoodItem by viewModel.selectedFoodItem.collectAsState()
    val trackingOrderId by viewModel.trackingOrderId.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCuisineFilter by viewModel.selectedCuisineFilter.collectAsState()
    val appliedPromoCode by viewModel.appliedPromoCode.collectAsState()
    val promoDiscountPercent by viewModel.promoDiscountPercent.collectAsState()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsState()

    val activeBanner by viewModel.activeBanner.collectAsState()
    val simulationSpeed by viewModel.simulationSpeedMultiplier.collectAsState()
    val isSimulationPaused by viewModel.isSimulationPaused.collectAsState()

    val isLocationLoading by viewModel.isLocationLoading.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()

    val allRestaurants by viewModel.allRestaurants.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val activeOrder by viewModel.activeOrder.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartSubtotal by viewModel.cartSubtotal.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    val defaultAddress by viewModel.defaultAddress.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()

    var showAddressSheet by remember { mutableStateOf(false) }

    // System Back Handler logic
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        when (currentScreen) {
            AppScreen.RESTAURANT_DETAIL -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.CART_CHECKOUT -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.TRACKING -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.ORDERS -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.NOTIFICATIONS -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.ADDRESSES -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.HOME -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != AppScreen.RESTAURANT_DETAIL) {
                AppBottomNavigationBar(
                    currentScreen = currentScreen,
                    cartCount = cartItems.sumOf { it.quantity },
                    unreadNotificationCount = unreadNotificationsCount,
                    hasActiveOrder = activeOrder != null && activeOrder?.status != OrderStatus.DELIVERED && activeOrder?.status != OrderStatus.CANCELLED,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen Routing
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        restaurants = allRestaurants,
                        activeOrder = activeOrder,
                        currentAddress = defaultAddress,
                        isLocationLoading = isLocationLoading,
                        hasLocationPermission = hasLocationPermission,
                        searchQuery = searchQuery,
                        selectedCuisine = selectedCuisineFilter,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onSelectCuisine = { viewModel.setCuisineFilter(it) },
                        onSelectRestaurant = { restaurantId ->
                            viewModel.selectRestaurant(restaurantId)
                        },
                        onViewActiveOrder = { orderId ->
                            viewModel.viewOrderTracking(orderId)
                        },
                        onAddressClick = { showAddressSheet = true },
                        onLocateMeClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                            viewModel.requestLocationAndFetchNearby()
                        },
                        onQuickDemoOrder = { viewModel.placeQuickDemoOrder() }
                    )
                }

                AppScreen.RESTAURANT_DETAIL -> {
                    val restaurant = allRestaurants.find { it.id == selectedRestaurantId }
                        ?: allRestaurants.firstOrNull()
                        ?: return@Box

                    RestaurantDetailScreen(
                        restaurant = restaurant,
                        selectedFoodItem = selectedFoodItem,
                        cartItemCount = cartItems.sumOf { it.quantity },
                        cartSubtotal = cartSubtotal,
                        onBack = { viewModel.navigateTo(AppScreen.HOME) },
                        onFoodItemClick = { item -> viewModel.openFoodItemDetails(item) },
                        onDismissFoodDetails = { viewModel.closeFoodItemDetails() },
                        onAddToCart = { item, rest, qty, customs ->
                            viewModel.addToCart(item, rest, qty, customs)
                        },
                        onViewCart = { viewModel.navigateTo(AppScreen.CART_CHECKOUT) }
                    )
                }

                AppScreen.CART_CHECKOUT -> {
                    CartCheckoutScreen(
                        cartItems = cartItems,
                        subtotal = cartSubtotal,
                        promoCode = appliedPromoCode,
                        promoDiscountPercent = promoDiscountPercent,
                        selectedPaymentMethod = selectedPaymentMethod,
                        deliveryAddress = defaultAddress,
                        onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                        onClearCart = { viewModel.clearCart() },
                        onApplyPromo = { code -> viewModel.applyPromoCode(code) },
                        onRemovePromo = { viewModel.removePromoCode() },
                        onSelectPayment = { method -> viewModel.setPaymentMethod(method) },
                        onChangeAddress = { showAddressSheet = true },
                        onCheckout = {
                            viewModel.checkoutAndPlaceOrder { _ -> }
                        },
                        onBrowseFood = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }

                AppScreen.TRACKING -> {
                    val currentTrackingOrder = allOrders.find { it.id == trackingOrderId }
                        ?: activeOrder
                        ?: allOrders.firstOrNull()

                    LiveTrackingScreen(
                        order = currentTrackingOrder,
                        simulationSpeed = simulationSpeed,
                        isPaused = isSimulationPaused,
                        onSpeedChange = { speed -> viewModel.setSimulationSpeed(speed) },
                        onTogglePause = { viewModel.toggleSimulationPause() },
                        onFastForward = { orderId -> viewModel.fastForwardTracking(orderId) },
                        onCancelOrder = { orderId -> viewModel.cancelOrder(orderId) },
                        onStartDemoOrder = { viewModel.placeQuickDemoOrder() }
                    )
                }

                AppScreen.ORDERS -> {
                    OrdersHistoryScreen(
                        orders = allOrders,
                        onTrackOrder = { orderId -> viewModel.viewOrderTracking(orderId) },
                        onBrowseFood = { viewModel.navigateTo(AppScreen.HOME) },
                        onStartDemoOrder = { viewModel.placeQuickDemoOrder() }
                    )
                }

                AppScreen.NOTIFICATIONS -> {
                    NotificationsScreen(
                        notifications = notifications,
                        unreadCount = unreadNotificationsCount,
                        onNotificationClick = { notif ->
                            viewModel.markNotificationAsRead(notif.id)
                            if (notif.orderId != null) {
                                viewModel.viewOrderTracking(notif.orderId)
                            }
                        },
                        onMarkAllRead = { viewModel.markAllNotificationsAsRead() },
                        onClearAll = { viewModel.clearAllNotifications() },
                        onStartDemoOrder = { viewModel.placeQuickDemoOrder() }
                    )
                }

                AppScreen.ADDRESSES -> {
                    showAddressSheet = true
                    viewModel.navigateTo(AppScreen.HOME)
                }
            }

            // In-App Notification Alert Banner Popup
            activeBanner?.let { banner ->
                NotificationBanner(
                    banner = banner,
                    onDismiss = { viewModel.dismissBanner() },
                    onClick = {
                        viewModel.dismissBanner()
                        val currentActive = activeOrder
                        if (currentActive != null) {
                            viewModel.viewOrderTracking(currentActive.id)
                        } else {
                            viewModel.navigateTo(AppScreen.NOTIFICATIONS)
                        }
                    }
                )
            }

            // Address Management Bottom Sheet
            if (showAddressSheet) {
                AddressManagementBottomSheet(
                    addresses = savedAddresses,
                    onDismiss = { showAddressSheet = false },
                    onSelectAddress = { addressId ->
                        viewModel.setDefaultAddress(addressId)
                    },
                    onAddNewAddress = { label, line, notes ->
                        viewModel.addNewAddress(label, line, notes)
                    }
                )
            }
        }
    }
}
