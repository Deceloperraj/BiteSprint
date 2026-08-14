package com.example.data.repository

import android.location.Location
import com.example.R
import com.example.data.model.CustomizationItem
import com.example.data.model.CustomizationOption
import com.example.data.model.FoodItem
import com.example.data.model.MenuCategory
import com.example.data.model.Restaurant
import com.example.data.sample.MockRestaurantData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.random.Random

class NearbyRestaurantRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchNearbyRestaurants(
        userLat: Double,
        userLng: Double,
        city: String = ""
    ): List<Restaurant> = withContext(Dispatchers.IO) {
        try {
            // Query real restaurants & cafes in a ~3km radius around user GPS coordinates
            val query = """
                [out:json][timeout:6];
                (
                  node["amenity"~"restaurant|cafe|fast_food"](around:3500,$userLat,$userLng);
                  way["amenity"~"restaurant|cafe|fast_food"](around:3500,$userLat,$userLng);
                );
                out center 15;
            """.trimIndent()

            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter?data=${java.net.URLEncoder.encode(query, "UTF-8")}")
                .header("User-Agent", "BiteSprint-FoodDelivery/1.0 (Android)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string()
                if (!jsonString.isNullOrBlank()) {
                    val root = JSONObject(jsonString)
                    val elements = root.optJSONArray("elements")
                    if (elements != null && elements.length() > 0) {
                        val parsed = mutableListOf<Restaurant>()

                        for (i in 0 until elements.length()) {
                            val el = elements.getJSONObject(i)
                            val tags = el.optJSONObject("tags") ?: continue
                            val name = tags.optString("name")
                            if (name.isNullOrBlank()) continue

                            val lat = if (el.has("lat")) el.optDouble("lat") else el.optJSONObject("center")?.optDouble("lat") ?: userLat
                            val lng = if (el.has("lon")) el.optDouble("lon") else el.optJSONObject("center")?.optDouble("lon") ?: userLng

                            val cuisineRaw = tags.optString("cuisine", tags.optString("amenity", "Multi-Cuisine"))
                            val street = tags.optString("addr:street", tags.optString("addr:place", "Near Current Location"))
                            val houseNumber = tags.optString("addr:housenumber", "")
                            val addressFull = if (street.isNotBlank()) "$houseNumber $street, $city".trim() else "Near $city"

                            // Calculate actual distance
                            val distResults = FloatArray(1)
                            Location.distanceBetween(userLat, userLng, lat, lng, distResults)
                            val distanceKm = (distResults[0] / 1000.0).let {
                                (it * 10).roundToInt() / 10.0
                            }.coerceAtLeast(0.4)

                            val estimatedMinutes = (15 + (distanceKm * 4.5).toInt()).coerceIn(12, 45)
                            val deliveryFeeRupees = (25.0 + (distanceKm * 8.0)).let {
                                (it / 5).roundToInt() * 5.0
                            }.coerceIn(20.0, 75.0)

                            val rating = 4.2 + (Random(name.hashCode()).nextInt(8) / 10.0)
                            val reviewCount = 80 + Random(name.hashCode()).nextInt(1200)

                            val generatedCategories = generateMenuForCuisine(name, cuisineRaw)
                            val heroDrawable = MockRestaurantData.getRestaurantDrawable(name, cuisineRaw)

                            val rest = Restaurant(
                                id = "live_rest_${el.optLong("id", i.toLong())}",
                                name = name,
                                cuisine = formatCuisine(cuisineRaw),
                                rating = (rating * 10).roundToInt() / 10.0,
                                reviewCount = reviewCount,
                                deliveryTimeMinutes = estimatedMinutes,
                                deliveryFee = deliveryFeeRupees,
                                minOrder = 149.0,
                                distanceKm = distanceKm,
                                heroDrawableRes = heroDrawable,
                                address = addressFull,
                                lat = lat,
                                lng = lng,
                                tags = listOf("GPS Verified", "Live Map", "${distanceKm} km away"),
                                promoBadge = if (distanceKm < 1.5) "Free Delivery over ₹199" else "₹50 OFF on ₹299+",
                                menuCategories = generatedCategories
                            )
                            parsed.add(rest)
                        }

                        if (parsed.isNotEmpty()) {
                            return@withContext parsed.sortedBy { it.distanceKm }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Network fallback or timeout
        }

        // Graceful fallback with localized distances & coordinates shifted around user's GPS
        return@withContext MockRestaurantData.SAMPLE_RESTAURANTS.mapIndexed { index, sample ->
            val angle = (index * 90.0) * (Math.PI / 180.0)
            val offsetDist = 0.008 + (index * 0.006) // ~0.8 to 2.5 km
            val restLat = userLat + (offsetDist * Math.cos(angle))
            val restLng = userLng + (offsetDist * Math.sin(angle))

            val distResults = FloatArray(1)
            Location.distanceBetween(userLat, userLng, restLat, restLng, distResults)
            val distanceKm = (distResults[0] / 1000.0).let {
                (it * 10).roundToInt() / 10.0
            }.coerceAtLeast(0.5)

            val deliveryTime = (15 + (distanceKm * 5).toInt()).coerceIn(15, 40)
            val deliveryFee = (25.0 + (distanceKm * 7.0)).let { (it / 5).roundToInt() * 5.0 }.coerceIn(25.0, 60.0)

            sample.copy(
                lat = restLat,
                lng = restLng,
                distanceKm = distanceKm,
                deliveryTimeMinutes = deliveryTime,
                deliveryFee = deliveryFee,
                address = if (city.isNotBlank()) "Near $city Center" else sample.address
            )
        }
    }

    private fun formatCuisine(raw: String): String {
        return raw.split(";", ",")
            .map { it.trim().replaceFirstChar { c -> c.uppercase() } }
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" • ")
            .ifBlank { "Multi-Cuisine • Gourmet" }
    }

    private fun generateMenuForCuisine(restaurantName: String, cuisine: String): List<MenuCategory> {
        val lower = "$restaurantName $cuisine".lowercase()

        return when {
            lower.contains("pizza") || lower.contains("italian") -> listOf(
                MenuCategory(
                    name = "Artisan Pizzas",
                    items = listOf(
                        FoodItem("pz_1", "Margherita Special", "San Marzano tomatoes, fresh mozzarella, basil & extra virgin olive oil.", 299.0, 720, 4.9, true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_pizza_italian,
                            availableCustomizations = listOf(CustomizationOption("Crust", listOf(CustomizationItem("Classic Hand Tossed", 0.0), CustomizationItem("Cheese Burst", 75.0))))
                        ),
                        FoodItem("pz_2", "Paneer Makhani Pizza", "Spiced cottage cheese, roasted capsicum, onion, makhani sauce.", 349.0, 810, 4.8, true, isSpicy = true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_pizza_italian
                        ),
                        FoodItem("pz_3", "Smoked Chicken Pepperoni", "Artisan chicken pepperoni, jalapeños, mozzarella cheese.", 399.0, 850, 4.9, false, isSpicy = true,
                            imageDrawableRes = R.drawable.img_pizza_italian
                        )
                    )
                ),
                MenuCategory(
                    name = "Sides & Beverages",
                    items = listOf(
                        FoodItem("sd_1", "Cheesy Stuffed Garlic Bread", "Baked garlic breadsticks with herb butter and mozzarella.", 159.0, 380, 4.8, true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_pizza_italian
                        ),
                        FoodItem("sd_2", "Chilled Cold Coffee", "Rich blended arabica cold coffee with chocolate syrup.", 129.0, 240, 4.9, true,
                            imageDrawableRes = R.drawable.img_hero_food
                        )
                    )
                )
            )

            lower.contains("burger") || lower.contains("fast_food") || lower.contains("american") -> listOf(
                MenuCategory(
                    name = "Signature Burgers",
                    items = listOf(
                        FoodItem("bg_1", "Crispy Aloo Tikki Cheese Burger", "Spiced potato patty, tangy mint mayo, sliced tomatoes, toasted brioche.", 129.0, 520, 4.8, true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_hero_food
                        ),
                        FoodItem("bg_2", "Double Smashed Chicken Beast", "Two smashed chicken patties, melted cheddar, caramelized onions.", 249.0, 760, 4.9, false, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_hero_food
                        ),
                        FoodItem("bg_3", "Peri Peri Paneer Supreme", "Crispy crumbed paneer, peri peri glaze, crunchy lettuce.", 199.0, 640, 4.7, true, isSpicy = true,
                            imageDrawableRes = R.drawable.img_hero_food
                        )
                    )
                ),
                MenuCategory(
                    name = "Sides & Shakes",
                    items = listOf(
                        FoodItem("fr_1", "Peri Peri French Fries", "Crisp golden fries tossed with zesty peri peri seasonings.", 119.0, 360, 4.8, true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_hero_food
                        ),
                        FoodItem("sh_1", "Belgian Chocolate Milkshake", "Thick blended chocolate shake topped with choco chips.", 169.0, 420, 4.9, true,
                            imageDrawableRes = R.drawable.img_hero_food
                        )
                    )
                )
            )

            lower.contains("ramen") || lower.contains("japanese") || lower.contains("asian") || lower.contains("chinese") -> listOf(
                MenuCategory(
                    name = "Signature Ramen & Bowls",
                    items = listOf(
                        FoodItem("rm_1", "Tokyo Shoyu Chicken Ramen", "Rich broth, ramen noodles, marinated soft egg, nori, bamboo shoots.", 399.0, 680, 4.9, false, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_ramen_bowl
                        ),
                        FoodItem("rm_2", "Spicy Shiitake & Tofu Ramen", "Mushroom broth, crispy silken tofu, bok choy, scallions and chili oil.", 349.0, 540, 4.8, true, isSpicy = true,
                            imageDrawableRes = R.drawable.img_ramen_bowl
                        )
                    )
                )
            )

            lower.contains("cafe") || lower.contains("coffee") || lower.contains("bakery") -> listOf(
                MenuCategory(
                    name = "Brews & Beverages",
                    items = listOf(
                        FoodItem("cf_1", "Hazelnut Iced Latte", "Double shot espresso over chilled hazelnut milk and foam.", 189.0, 160, 4.9, true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_hero_food
                        ),
                        FoodItem("cf_2", "Classic Cappuccino", "Steamed velvety microfoam over single origin espresso.", 149.0, 120, 4.8, true,
                            imageDrawableRes = R.drawable.img_hero_food
                        )
                    )
                ),
                MenuCategory(
                    name = "Bites & Pastries",
                    items = listOf(
                        FoodItem("bk_1", "Butter Croissant", "Flaky layered French butter pastry freshly baked.", 139.0, 280, 4.8, true,
                            imageDrawableRes = R.drawable.img_paneer_tikka
                        ),
                        FoodItem("bk_2", "Dark Chocolate Brownie", "Warm fudgy chocolate brownie with dark chocolate drizzle.", 149.0, 350, 4.9, true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_paneer_tikka
                        )
                    )
                )
            )

            else -> listOf(
                MenuCategory(
                    name = "Chef's Specials & Curries",
                    items = listOf(
                        FoodItem("ind_1", "Paneer Butter Masala", "Cottage cheese cubes in rich tomato, butter, and cashew gravy.", 269.0, 520, 4.9, true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_biryani_curry,
                            availableCustomizations = listOf(CustomizationOption("Spice Level", listOf(CustomizationItem("Mild", 0.0), CustomizationItem("Medium", 0.0), CustomizationItem("Spicy", 0.0))))
                        ),
                        FoodItem("ind_2", "Chicken Tikka Dum Biryani", "Basmati rice layered with spiced marinated chicken and saffron dum.", 319.0, 710, 4.9, false, isSpicy = true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_biryani_curry
                        ),
                        FoodItem("ind_3", "Paneer Tikka Angara Platter", "Char-grilled cottage cheese cubes with mint chutney & onions.", 279.0, 440, 4.9, true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_paneer_tikka
                        ),
                        FoodItem("ind_4", "Dal Makhani Special", "Slow cooked black lentils with cream and aromatic herbs.", 229.0, 460, 4.8, true,
                            imageDrawableRes = R.drawable.img_biryani_curry
                        )
                    )
                ),
                MenuCategory(
                    name = "Breads & Desserts",
                    items = listOf(
                        FoodItem("br_1", "Garlic Butter Naan", "Clay oven baked flatbread with roasted garlic and butter.", 55.0, 180, 4.9, true,
                            imageDrawableRes = R.drawable.img_biryani_curry
                        ),
                        FoodItem("ds_1", "Gulab Jamun with Rabri", "Two golden fried khoya dumplings soaked in rose syrup.", 119.0, 320, 4.9, true, isBestSeller = true,
                            imageDrawableRes = R.drawable.img_paneer_tikka
                        )
                    )
                )
            )
        }
    }
}
