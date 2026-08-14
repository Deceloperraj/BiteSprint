package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OrderEntity
import com.example.data.model.OrderStatus
import com.example.ui.components.LiveMapCanvas
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.OrangePrimary

@Composable
fun LiveTrackingScreen(
    order: OrderEntity?,
    simulationSpeed: Float,
    isPaused: Boolean,
    onSpeedChange: (Float) -> Unit,
    onTogglePause: () -> Unit,
    onFastForward: (Long) -> Unit,
    onCancelOrder: (Long) -> Unit,
    onStartDemoOrder: () -> Unit
) {
    val context = LocalContext.current
    var showCallModal by remember { mutableStateOf(false) }
    var showChatModal by remember { mutableStateOf(false) }
    var isOrderSummaryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        if (order == null) {
            // No Active Order State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricScooter,
                            contentDescription = "No active tracking",
                            tint = OrangePrimary,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Active Delivery",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "You don't have an order currently in delivery. Start a quick demo order to test real-time tracking and push notifications!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onStartDemoOrder,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier.testTag("start_demo_tracking_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Live Demo Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Status Card
                item {
                    TrackingHeaderCard(order = order)
                }

                // Interactive Live Vector Map Canvas
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    LiveMapCanvas(
                        restaurantLat = order.restaurantLat,
                        restaurantLng = order.restaurantLng,
                        customerLat = order.customerLat,
                        customerLng = order.customerLng,
                        driverLat = order.currentDriverLat,
                        driverLng = order.currentDriverLng,
                        orderStatus = order.status,
                        simulationProgress = order.simulationProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                }

                // Timeline Progress Stages
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OrderProgressTimelineCard(status = order.status)
                }

                // Courier Profile Card (When assigned / delivering)
                if (order.status != OrderStatus.CANCELLED) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        CourierProfileCard(
                            driverName = order.driverName,
                            driverRating = order.driverRating,
                            driverVehicle = order.driverVehicle,
                            driverPhone = order.driverPhone,
                            isDelivered = order.status == OrderStatus.DELIVERED,
                            onCall = { showCallModal = true },
                            onMessage = { showChatModal = true }
                        )
                    }
                }

                // Simulation Speed & Fast-Forward Controls
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SimulationControlsCard(
                        orderId = order.id,
                        orderStatus = order.status,
                        simulationSpeed = simulationSpeed,
                        isPaused = isPaused,
                        onSpeedChange = onSpeedChange,
                        onTogglePause = onTogglePause,
                        onFastForward = { onFastForward(order.id) },
                        onCancelOrder = { onCancelOrder(order.id) }
                    )
                }

                // Order Details Accordion
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isOrderSummaryExpanded = !isOrderSummaryExpanded },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = "Order Summary",
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Order Summary (${order.orderNumber})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Icon(
                                    imageVector = if (isOrderSummaryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand"
                                )
                            }

                            AnimatedVisibility(visible = isOrderSummaryExpanded) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    Text(
                                        text = "Restaurant: ${order.restaurantName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Items: ${order.itemsSummary}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Delivery Address: ${order.deliveryAddress}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Total Paid: ₹${order.totalAmount.toInt()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangePrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }

    // Mock Call Driver Dialog
    if (showCallModal && order != null) {
        AlertDialog(
            onDismissRequest = { showCallModal = false },
            title = { Text("Call ${order.driverName}") },
            text = {
                Column {
                    Text("Dialing ${order.driverPhone} (${order.driverVehicle})")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "\"Hello! I'm on the way with your food from ${order.restaurantName}! ETA is ~${order.etaMinutes} mins.\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCallModal = false
                        Toast.makeText(context, "Call completed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("End Call")
                }
            }
        )
    }

    // Mock Message Driver Dialog
    if (showChatModal && order != null) {
        AlertDialog(
            onDismissRequest = { showChatModal = false },
            title = { Text("Chat with ${order.driverName}") },
            text = {
                Column {
                    Text("Delivery Instructions: ${order.deliveryAddress}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Courier: \"I've picked up your order and following the fastest route. See you soon!\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showChatModal = false
                        Toast.makeText(context, "Note sent to courier", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun TrackingHeaderCard(order: OrderEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tracking_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                    Text(
                        text = order.restaurantName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (order.status) {
                        OrderStatus.DELIVERED -> FreshGreen.copy(alpha = 0.15f)
                        OrderStatus.CANCELLED -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        else -> OrangePrimary.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = order.status.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (order.status) {
                            OrderStatus.DELIVERED -> FreshGreen
                            OrderStatus.CANCELLED -> Color(0xFFEF4444)
                            else -> OrangePrimary
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (order.status == OrderStatus.DELIVERED) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Delivered",
                        tint = FreshGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Delivered at ${order.deliveryAddress}",
                            fontWeight = FontWeight.Bold,
                            color = FreshGreen,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Enjoy your fresh and hot meal!",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            } else if (order.status == OrderStatus.CANCELLED) {
                Text(
                    text = "This order was cancelled.",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "ETA",
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Estimated Arrival: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "~${order.etaMinutes} mins",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OrangePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun OrderProgressTimelineCard(status: OrderStatus) {
    val steps = listOf(
        OrderStatus.PLACED to "Placed",
        OrderStatus.CONFIRMED to "Confirmed",
        OrderStatus.PREPARING to "Preparing",
        OrderStatus.OUT_FOR_DELIVERY to "On the Way",
        OrderStatus.DELIVERED to "Delivered"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Order Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, (stepStatus, label) ->
                    val isCompleted = status.stepIndex >= stepStatus.stepIndex && status != OrderStatus.CANCELLED
                    val isCurrent = status == stepStatus

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    when {
                                        isCompleted -> OrangePrimary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourierProfileCard(
    driverName: String,
    driverRating: Double,
    driverVehicle: String,
    driverPhone: String,
    isDelivered: Boolean,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE082)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛵", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = driverName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = String.format("%.2f", driverRating),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = driverVehicle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            if (!isDelivered) {
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(38.dp)
                        .background(OrangePrimary.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onMessage,
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Message",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SimulationControlsCard(
    orderId: Long,
    orderStatus: OrderStatus,
    simulationSpeed: Float,
    isPaused: Boolean,
    onSpeedChange: (Float) -> Unit,
    onTogglePause: () -> Unit,
    onFastForward: () -> Unit,
    onCancelOrder: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simulation_controls_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Tracking Simulator",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Speed buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1f to "1x", 2f to "2x", 4f to "4x").forEach { (speed, label) ->
                        val isSelected = simulationSpeed == speed
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable { onSpeedChange(speed) }
                                .padding(2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (orderStatus != OrderStatus.DELIVERED && orderStatus != OrderStatus.CANCELLED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTogglePause,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause/Resume",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPaused) "Resume" else "Pause", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onFastForward,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Next Stage",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Next Stage", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCancelOrder,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(imageVector = Icons.Default.Cancel, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Text(
                    text = "Delivery simulation ended. You can place another order from the Explore menu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
