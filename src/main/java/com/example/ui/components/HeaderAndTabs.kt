package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedAddressEntity
import com.example.ui.AppScreen
import com.example.ui.theme.OrangePrimary

@Composable
fun AppBottomNavigationBar(
    currentScreen: AppScreen,
    cartCount: Int,
    unreadNotificationCount: Int,
    hasActiveOrder: Boolean,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.HOME || currentScreen == AppScreen.RESTAURANT_DETAIL,
            onClick = { onNavigate(AppScreen.HOME) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.HOME) Icons.Filled.Restaurant else Icons.Outlined.Restaurant,
                    contentDescription = "Explore"
                )
            },
            label = { Text("Explore", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = OrangePrimary.copy(alpha = 0.15f),
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary
            ),
            modifier = Modifier.testTag("nav_tab_explore")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.TRACKING,
            onClick = { onNavigate(AppScreen.TRACKING) },
            icon = {
                BadgedBox(
                    badge = {
                        if (hasActiveOrder) {
                            Badge(
                                containerColor = OrangePrimary,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ElectricScooter,
                        contentDescription = "Track"
                    )
                }
            },
            label = { Text("Live Track", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = OrangePrimary.copy(alpha = 0.15f),
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary
            ),
            modifier = Modifier.testTag("nav_tab_tracking")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.CART_CHECKOUT,
            onClick = { onNavigate(AppScreen.CART_CHECKOUT) },
            icon = {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(
                                containerColor = OrangePrimary,
                                contentColor = Color.White
                            ) {
                                Text("$cartCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentScreen == AppScreen.CART_CHECKOUT) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                        contentDescription = "Cart"
                    )
                }
            },
            label = { Text("Cart", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = OrangePrimary.copy(alpha = 0.15f),
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary
            ),
            modifier = Modifier.testTag("nav_tab_cart")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.ORDERS,
            onClick = { onNavigate(AppScreen.ORDERS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.ORDERS) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = "Orders"
                )
            },
            label = { Text("Orders", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = OrangePrimary.copy(alpha = 0.15f),
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary
            ),
            modifier = Modifier.testTag("nav_tab_orders")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.NOTIFICATIONS,
            onClick = { onNavigate(AppScreen.NOTIFICATIONS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadNotificationCount > 0) {
                            Badge(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ) {
                                Text("$unreadNotificationCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentScreen == AppScreen.NOTIFICATIONS) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        contentDescription = "Alerts"
                    )
                }
            },
            label = { Text("Alerts", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = OrangePrimary.copy(alpha = 0.15f),
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary
            ),
            modifier = Modifier.testTag("nav_tab_alerts")
        )
    }
}

@Composable
fun TopLocationHeader(
    currentAddress: SavedAddressEntity?,
    isLocationLoading: Boolean = false,
    onLocateMeClick: () -> Unit = {},
    onAddressClick: () -> Unit,
    onQuickDemoOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onAddressClick() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = OrangePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Deliver to ${currentAddress?.label ?: "GPS Location"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select address",
                    tint = OrangePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = if (isLocationLoading) "Locating via GPS..." else (currentAddress?.addressLine ?: "Indiranagar, Bengaluru"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // GPS Locate Icon Button
        Surface(
            shape = CircleShape,
            color = if (isLocationLoading) OrangePrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .size(36.dp)
                .clickable { onLocateMeClick() }
                .testTag("gps_locate_me_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Locate GPS",
                    tint = OrangePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Quick Demo Order Shortcut Button
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OrangePrimary,
            contentColor = Color.White,
            modifier = Modifier
                .clickable { onQuickDemoOrder() }
                .testTag("quick_demo_order_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricScooter,
                    contentDescription = "Demo Order",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Demo Order",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CuisineFilterChipsRow(
    selectedCuisine: String,
    onSelectCuisine: (String) -> Unit
) {
    val cuisines = listOf(
        "All" to "🍽️",
        "Pizza" to "🍕",
        "Burgers" to "🍔",
        "Ramen" to "🍜",
        "Healthy" to "🥗",
        "Desserts" to "🍰"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cuisines.forEach { (name, emoji) ->
            val isSelected = selectedCuisine.equals(name, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onSelectCuisine(name) }
                    .testTag("filter_chip_$name")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = emoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
