package com.example.data.model

import com.example.R

data class Restaurant(
    val id: String,
    val name: String,
    val cuisine: String,
    val rating: Double,
    val reviewCount: Int,
    val deliveryTimeMinutes: Int,
    val deliveryFee: Double,
    val minOrder: Double,
    val distanceKm: Double,
    val heroDrawableRes: Int,
    val address: String,
    val lat: Double,
    val lng: Double,
    val tags: List<String>,
    val menuCategories: List<MenuCategory>,
    val isOpen: Boolean = true,
    val promoBadge: String? = null
)

data class MenuCategory(
    val name: String,
    val items: List<FoodItem>
)

data class FoodItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val calories: Int,
    val rating: Double,
    val isVeg: Boolean,
    val isSpicy: Boolean = false,
    val isBestSeller: Boolean = false,
    val imageDrawableRes: Int = R.drawable.img_hero_food,
    val availableCustomizations: List<CustomizationOption> = emptyList()
)

data class CustomizationOption(
    val title: String,
    val options: List<CustomizationItem>,
    val isMultiSelect: Boolean = false
)

data class CustomizationItem(
    val name: String,
    val extraPrice: Double = 0.0
)

enum class OrderStatus(val displayName: String, val stepIndex: Int, val description: String) {
    PLACED("Order Placed", 0, "Your order has been sent to the restaurant"),
    CONFIRMED("Confirmed", 1, "The restaurant accepted your order"),
    PREPARING("Preparing Food", 2, "Chef is crafting your delicious meal"),
    OUT_FOR_DELIVERY("Out for Delivery", 3, "Courier is on the way to your door"),
    DELIVERED("Delivered", 4, "Meal delivered! Enjoy your food!"),
    CANCELLED("Cancelled", -1, "Order was cancelled")
}

data class CartItemUi(
    val id: Long = 0,
    val restaurantId: String,
    val restaurantName: String,
    val foodItemId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val selectedOptions: String = ""
)

data class Waypoint(
    val lat: Double,
    val lng: Double,
    val label: String = ""
)
