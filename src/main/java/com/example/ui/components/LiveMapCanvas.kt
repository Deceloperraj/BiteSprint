package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.tracking.TrackingEngine

@Composable
fun LiveMapCanvas(
    restaurantLat: Double,
    restaurantLng: Double,
    customerLat: Double,
    customerLng: Double,
    driverLat: Double,
    driverLng: Double,
    orderStatus: OrderStatus,
    simulationProgress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mapPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 48f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE8ECEF))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw Map Background & City Grid Structure
            drawRect(color = Color(0xFFE5E9EC), size = size)

            // Parks and Greenery
            drawRoundRect(
                color = Color(0xFFD3E7D5),
                topLeft = Offset(width * 0.08f, height * 0.12f),
                size = Size(width * 0.28f, height * 0.22f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = Color(0xFFD3E7D5),
                topLeft = Offset(width * 0.65f, height * 0.60f),
                size = Size(width * 0.28f, height * 0.25f),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // River / Canal curve
            val riverPath = Path().apply {
                moveTo(0f, height * 0.40f)
                cubicTo(
                    width * 0.35f, height * 0.35f,
                    width * 0.65f, height * 0.50f,
                    width, height * 0.42f
                )
            }
            drawPath(
                path = riverPath,
                color = Color(0xFFC3DCF5),
                style = Stroke(width = 24f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // City Street Grid Lines
            val roadColor = Color(0xFFFFFFFF)
            val secondaryRoadColor = Color(0xFFF0F3F5)

            // Horizontal Secondary roads
            for (i in 1..8) {
                val y = height * (i / 9f)
                drawLine(
                    color = secondaryRoadColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 6f
                )
            }

            // Vertical Secondary roads
            for (i in 1..6) {
                val x = width * (i / 7f)
                drawLine(
                    color = secondaryRoadColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 6f
                )
            }

            // Main Arterial Avenue (Diagonal)
            drawLine(
                color = roadColor,
                start = Offset(0f, height * 0.85f),
                end = Offset(width, height * 0.15f),
                strokeWidth = 14f,
                cap = StrokeCap.Round
            )
            // Main Cross Avenue
            drawLine(
                color = roadColor,
                start = Offset(width * 0.20f, 0f),
                end = Offset(width * 0.80f, height),
                strokeWidth = 14f,
                cap = StrokeCap.Round
            )

            // 2. Define Anchor Coordinates on Canvas
            val startPoint = Offset(width * 0.22f, height * 0.72f) // Restaurant
            val waypoint1 = Offset(width * 0.42f, height * 0.55f)
            val waypoint2 = Offset(width * 0.58f, height * 0.42f)
            val endPoint = Offset(width * 0.78f, height * 0.22f)   // Customer Home

            // Full Route Path
            val fullRoutePath = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                lineTo(waypoint1.x, waypoint1.y)
                lineTo(waypoint2.x, waypoint2.y)
                lineTo(endPoint.x, endPoint.y)
            }

            // Background dashed trajectory
            drawPath(
                path = fullRoutePath,
                color = Color(0xFF94A3B8),
                style = Stroke(
                    width = 8f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            )

            // Active completed delivery path
            val curProgress = simulationProgress.coerceIn(0f, 1f)
            val driverPos = when {
                curProgress < 0.33f -> {
                    val p = curProgress / 0.33f
                    Offset(
                        startPoint.x + (waypoint1.x - startPoint.x) * p,
                        startPoint.y + (waypoint1.y - startPoint.y) * p
                    )
                }
                curProgress < 0.66f -> {
                    val p = (curProgress - 0.33f) / 0.33f
                    Offset(
                        waypoint1.x + (waypoint2.x - waypoint1.x) * p,
                        waypoint1.y + (waypoint2.y - waypoint1.y) * p
                    )
                }
                else -> {
                    val p = (curProgress - 0.66f) / 0.34f
                    Offset(
                        waypoint2.x + (endPoint.x - waypoint2.x) * p,
                        waypoint2.y + (endPoint.y - waypoint2.y) * p
                    )
                }
            }

            // Draw glowing active path traveled
            val completedPath = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                if (curProgress >= 0.33f) lineTo(waypoint1.x, waypoint1.y)
                if (curProgress >= 0.66f) lineTo(waypoint2.x, waypoint2.y)
                lineTo(driverPos.x, driverPos.y)
            }
            drawPath(
                path = completedPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF5722), Color(0xFFFF9800)),
                    start = startPoint,
                    end = endPoint
                ),
                style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 3. Draw Restaurant Marker
            drawMarker(
                center = startPoint,
                bgColor = Color(0xFF1E293B),
                borderColor = Color.White,
                iconColor = Color(0xFFFF9800),
                label = "Restaurant"
            )

            // 4. Draw Customer Destination Marker
            drawMarker(
                center = endPoint,
                bgColor = Color(0xFF10B981),
                borderColor = Color.White,
                iconColor = Color.White,
                label = "Delivery"
            )

            // 5. Draw Courier Live Position Marker & Pulse Waves
            if (orderStatus == OrderStatus.OUT_FOR_DELIVERY || orderStatus == OrderStatus.PREPARING || orderStatus == OrderStatus.CONFIRMED) {
                // Radar pulse rings
                drawCircle(
                    color = Color(0xFFFF5722).copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = driverPos
                )
                drawCircle(
                    color = Color(0xFFFF5722).copy(alpha = pulseAlpha * 0.5f),
                    radius = pulseRadius * 1.5f,
                    center = driverPos
                )

                // Courier Pin Shadow
                drawCircle(
                    color = Color(0x33000000),
                    radius = 20f,
                    center = Offset(driverPos.x, driverPos.y + 6f)
                )

                // Courier Pin Outer
                drawCircle(
                    color = Color.White,
                    radius = 18f,
                    center = driverPos
                )
                // Courier Pin Body
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFF7043), Color(0xFFE64A19)),
                        center = driverPos,
                        radius = 16f
                    ),
                    radius = 15f,
                    center = driverPos
                )

                // Draw Mini Directional Arrow inside pin
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = driverPos
                )
            } else if (orderStatus == OrderStatus.DELIVERED) {
                // Delivered Success Star Badge at destination
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.3f),
                    radius = 32f,
                    center = endPoint
                )
            }
        }
    }
}

private fun DrawScope.drawMarker(
    center: Offset,
    bgColor: Color,
    borderColor: Color,
    iconColor: Color,
    label: String
) {
    // Shadow
    drawCircle(
        color = Color(0x2A000000),
        radius = 18f,
        center = Offset(center.x, center.y + 4f)
    )
    // Outer border
    drawCircle(
        color = borderColor,
        radius = 16f,
        center = center
    )
    // Main fill
    drawCircle(
        color = bgColor,
        radius = 13f,
        center = center
    )
    // Core dot
    drawCircle(
        color = iconColor,
        radius = 5f,
        center = center
    )
}
