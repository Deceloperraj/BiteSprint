package com.example.data.sample

import com.example.R
import com.example.data.model.CustomizationItem
import com.example.data.model.CustomizationOption
import com.example.data.model.FoodItem
import com.example.data.model.MenuCategory
import com.example.data.model.Restaurant

object MockRestaurantData {

    fun getFoodDrawable(name: String, cuisine: String = ""): Int {
        val lower = "$name $cuisine".lowercase()
        return when {
            lower.contains("pizza") || lower.contains("pasta") || lower.contains("breadstick") || lower.contains("margherita") || lower.contains("pepperoni") -> R.drawable.img_pizza_italian
            lower.contains("ramen") || lower.contains("noodle") || lower.contains("broth") || lower.contains("shoyu") || lower.contains("japanese") || lower.contains("dim sum") -> R.drawable.img_ramen_bowl
            lower.contains("paneer tikka") || lower.contains("malai tikka") || lower.contains("starter") || lower.contains("kebab") || lower.contains("gulab jamun") -> R.drawable.img_paneer_tikka
            lower.contains("biryani") || lower.contains("curry") || lower.contains("makhani") || lower.contains("butter chicken") || lower.contains("dal") || lower.contains("naan") || lower.contains("masala") -> R.drawable.img_biryani_curry
            lower.contains("burger") || lower.contains("fries") || lower.contains("shake") || lower.contains("cajun") || lower.contains("brioche") || lower.contains("beast") -> R.drawable.img_hero_food
            else -> R.drawable.img_biryani_curry
        }
    }

    fun getRestaurantDrawable(name: String, cuisine: String = ""): Int {
        val lower = "$name $cuisine".lowercase()
        return when {
            lower.contains("pizza") || lower.contains("italian") || lower.contains("crust") -> R.drawable.img_pizza_italian
            lower.contains("ramen") || lower.contains("noodle") || lower.contains("japanese") || lower.contains("asian") || lower.contains("chinese") || lower.contains("katsu") -> R.drawable.img_ramen_bowl
            lower.contains("burger") || lower.contains("fries") || lower.contains("sizzle") || lower.contains("shake") || lower.contains("fast_food") || lower.contains("cafe") -> R.drawable.img_hero_food
            lower.contains("tikka") || lower.contains("punjab") || lower.contains("tandoor") || lower.contains("grill") -> R.drawable.img_paneer_tikka
            else -> R.drawable.img_biryani_curry
        }
    }

    val SAMPLE_RESTAURANTS: List<Restaurant> = listOf(
        Restaurant(
            id = "rest_1",
            name = "Punjab Grill & Tandoor",
            cuisine = "North Indian • Mughlai • Biryani",
            rating = 4.89,
            reviewCount = 1420,
            deliveryTimeMinutes = 25,
            deliveryFee = 35.0,
            minOrder = 199.0,
            distanceKm = 1.8,
            heroDrawableRes = R.drawable.img_biryani_curry,
            address = "45 Ring Road, City Center",
            lat = 28.6139,
            lng = 77.2090,
            tags = listOf("Popular", "Fast Delivery", "Top Rated"),
            promoBadge = "₹75 OFF over ₹399",
            menuCategories = listOf(
                MenuCategory(
                    name = "Tandoor & Starters",
                    items = listOf(
                        FoodItem(
                            id = "tan_1",
                            name = "Paneer Tikka Angara",
                            description = "Fresh cottage cheese marinated in spicy Kashmiri tandoori masala, grilled in clay oven with mint chutney.",
                            price = 280.0,
                            calories = 450,
                            rating = 4.95,
                            isVeg = true,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_paneer_tikka,
                            availableCustomizations = listOf(
                                CustomizationOption(
                                    title = "Portion Size",
                                    options = listOf(
                                        CustomizationItem("Regular (6 Pcs)", 0.0),
                                        CustomizationItem("Large Platter (10 Pcs)", 120.0)
                                    )
                                ),
                                CustomizationOption(
                                    title = "Add Extras",
                                    isMultiSelect = true,
                                    options = listOf(
                                        CustomizationItem("Extra Mint Chutney & Onions", 20.0),
                                        CustomizationItem("Butter Garlic Naan", 60.0)
                                    )
                                )
                            )
                        ),
                        FoodItem(
                            id = "tan_2",
                            name = "Murgh Malai Tikka",
                            description = "Succulent boneless chicken chunks marinated in cream, cashew paste, cardamom, and subtle spices.",
                            price = 360.0,
                            calories = 520,
                            rating = 4.91,
                            isVeg = false,
                            isSpicy = false,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_paneer_tikka
                        )
                    )
                ),
                MenuCategory(
                    name = "Main Course & Biryani",
                    items = listOf(
                        FoodItem(
                            id = "curry_1",
                            name = "Butter Chicken (Murgh Makhani)",
                            description = "Tender roasted chicken simmered in rich velvety tomato, butter, and cashew cream gravy.",
                            price = 390.0,
                            calories = 680,
                            rating = 4.98,
                            isVeg = false,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_biryani_curry
                        ),
                        FoodItem(
                            id = "curry_2",
                            name = "Dal Makhani Grand",
                            description = "Slow cooked black lentils overnight with fresh cream, butter, and aromatic spices.",
                            price = 290.0,
                            calories = 490,
                            rating = 4.86,
                            isVeg = true,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_biryani_curry
                        ),
                        FoodItem(
                            id = "biry_1",
                            name = "Hyderabadi Dum Biryani",
                            description = "Fragrant long grain basmati rice cooked on slow dum with aromatic spices, saffron, and tender cuts.",
                            price = 340.0,
                            calories = 720,
                            rating = 4.93,
                            isVeg = false,
                            isSpicy = true,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_biryani_curry
                        )
                    )
                ),
                MenuCategory(
                    name = "Breads & Desserts",
                    items = listOf(
                        FoodItem(
                            id = "bread_1",
                            name = "Butter Garlic Naan",
                            description = "Freshly baked clay oven naan brushed with roasted garlic and creamy butter.",
                            price = 65.0,
                            calories = 190,
                            rating = 4.88,
                            isVeg = true,
                            imageDrawableRes = R.drawable.img_biryani_curry
                        ),
                        FoodItem(
                            id = "des_1",
                            name = "Gulab Jamun with Rabri (2 Pcs)",
                            description = "Warm khoya dumplings soaked in rose cardamom sugar syrup topped with rich rabri.",
                            price = 140.0,
                            calories = 360,
                            rating = 4.96,
                            isVeg = true,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_paneer_tikka
                        )
                    )
                )
            )
        ),
        Restaurant(
            id = "rest_2",
            name = "FireCrust Wood-Fired Pizzeria",
            cuisine = "Italian • Artisan Pizza • Pastas",
            rating = 4.84,
            reviewCount = 980,
            deliveryTimeMinutes = 20,
            deliveryFee = 29.0,
            minOrder = 249.0,
            distanceKm = 1.2,
            heroDrawableRes = R.drawable.img_pizza_italian,
            address = "78 Gourmet Avenue",
            lat = 28.6180,
            lng = 77.2140,
            tags = listOf("Fast Delivery", "Cheesy Delights"),
            promoBadge = "20% OFF up to ₹100",
            menuCategories = listOf(
                MenuCategory(
                    name = "Signature Pizzas",
                    items = listOf(
                        FoodItem(
                            id = "pizz_1",
                            name = "Truffle Burrata Margherita",
                            description = "San Marzano tomato base, fresh burrata mozzarella, basil oil, and wood-fired charred crust.",
                            price = 450.0,
                            calories = 780,
                            rating = 4.92,
                            isVeg = true,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_pizza_italian,
                            availableCustomizations = listOf(
                                CustomizationOption(
                                    title = "Crust Style",
                                    options = listOf(
                                        CustomizationItem("Classic Hand Tossed", 0.0),
                                        CustomizationItem("Cheesy Burst Crust", 80.0),
                                        CustomizationItem("Thin & Crispy", 0.0)
                                    )
                                )
                            )
                        ),
                        FoodItem(
                            id = "pizz_2",
                            name = "Spicy Pepperoni & Jalapeno",
                            description = "Artisan smoked pepperoni, pickled jalapenos, hot honey drizzle, mozzarella.",
                            price = 499.0,
                            calories = 860,
                            rating = 4.88,
                            isVeg = false,
                            isSpicy = true,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_pizza_italian
                        )
                    )
                ),
                MenuCategory(
                    name = "Pastas & Sides",
                    items = listOf(
                        FoodItem(
                            id = "pasta_1",
                            name = "Creamy Truffle Penne Alfredo",
                            description = "Silky parmesan and cream sauce with sautéed wild mushrooms and garlic focaccia.",
                            price = 380.0,
                            calories = 620,
                            rating = 4.85,
                            isVeg = true,
                            imageDrawableRes = R.drawable.img_pizza_italian
                        ),
                        FoodItem(
                            id = "side_1",
                            name = "Cheesy Garlic Herb Breadsticks",
                            description = "Oven-baked breadsticks stuffed with mozzarella and herbs, served with marinara dip.",
                            price = 180.0,
                            calories = 340,
                            rating = 4.79,
                            isVeg = true,
                            imageDrawableRes = R.drawable.img_pizza_italian
                        )
                    )
                )
            )
        ),
        Restaurant(
            id = "rest_3",
            name = "Urban Sizzle Smash Burgers",
            cuisine = "American • Gourmet Burgers • Shakes",
            rating = 4.91,
            reviewCount = 2150,
            deliveryTimeMinutes = 18,
            deliveryFee = 25.0,
            minOrder = 149.0,
            distanceKm = 0.9,
            heroDrawableRes = R.drawable.img_hero_food,
            address = "12 High Street Hub",
            lat = 28.6110,
            lng = 77.2030,
            tags = listOf("Lightning Fast", "Bestseller"),
            promoBadge = "Free Coke on ₹299+",
            menuCategories = listOf(
                MenuCategory(
                    name = "Craft Burgers",
                    items = listOf(
                        FoodItem(
                            id = "burg_1",
                            name = "Double Smashed Cheese Beast",
                            description = "Double smashed crispy patties, melted aged cheddar, caramelized onions, house truffle secret sauce on toasted brioche.",
                            price = 320.0,
                            calories = 810,
                            rating = 4.96,
                            isVeg = false,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_hero_food
                        ),
                        FoodItem(
                            id = "burg_2",
                            name = "Crispy Peri-Peri Paneer Burger",
                            description = "Spiced crumb-fried cottage cheese patty, peri-peri drizzle, iceberg lettuce, smoked paprika mayo.",
                            price = 240.0,
                            calories = 640,
                            rating = 4.87,
                            isVeg = true,
                            isSpicy = true,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_hero_food
                        )
                    )
                ),
                MenuCategory(
                    name = "Sides & Shakes",
                    items = listOf(
                        FoodItem(
                            id = "fry_1",
                            name = "Peri Peri Cajun Fries",
                            description = "Crispy golden french fries tossed with zesty peri peri spices and cheese dip.",
                            price = 140.0,
                            calories = 390,
                            rating = 4.90,
                            isVeg = true,
                            imageDrawableRes = R.drawable.img_hero_food
                        ),
                        FoodItem(
                            id = "shk_1",
                            name = "Salted Belgian Chocolate Shake",
                            description = "Thick blended shake with rich Belgian chocolate fudge and whipped cream.",
                            price = 190.0,
                            calories = 490,
                            rating = 4.94,
                            isVeg = true,
                            imageDrawableRes = R.drawable.img_hero_food
                        )
                    )
                )
            )
        ),
        Restaurant(
            id = "rest_4",
            name = "Ramen Katsuya & Bowls",
            cuisine = "Japanese • Ramen • Dim Sum",
            rating = 4.82,
            reviewCount = 760,
            deliveryTimeMinutes = 30,
            deliveryFee = 40.0,
            minOrder = 299.0,
            distanceKm = 2.4,
            heroDrawableRes = R.drawable.img_ramen_bowl,
            address = "90 Lotus Commercial Complex",
            lat = 28.6220,
            lng = 77.2200,
            tags = listOf("Hot Broth Guaranteed", "Authentic"),
            promoBadge = "Flat ₹50 OFF",
            menuCategories = listOf(
                MenuCategory(
                    name = "Signature Ramen",
                    items = listOf(
                        FoodItem(
                            id = "ram_1",
                            name = "Tokyo Shoyu Chicken Ramen",
                            description = "Rich chicken broth, springy ramen noodles, soft ajitsuke tamago egg, nori, bamboo shoots, scallions.",
                            price = 420.0,
                            calories = 720,
                            rating = 4.93,
                            isVeg = false,
                            isBestSeller = true,
                            imageDrawableRes = R.drawable.img_ramen_bowl
                        ),
                        FoodItem(
                            id = "ram_2",
                            name = "Spicy Shiitake & Tofu Ramen",
                            description = "Fragrant roasted mushroom and sesame broth, crispy silken tofu, bok choy, chili oil.",
                            price = 380.0,
                            calories = 540,
                            rating = 4.85,
                            isVeg = true,
                            isSpicy = true,
                            imageDrawableRes = R.drawable.img_ramen_bowl
                        )
                    )
                )
            )
        )
    )
}
