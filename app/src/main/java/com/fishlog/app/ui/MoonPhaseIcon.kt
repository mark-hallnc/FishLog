package com.fishlog.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MoonPhaseIcon(
    illuminationPercent: Double?,
    waxing: Boolean?,
    phaseName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val description = if (phaseName != null && illuminationPercent != null) {
        "$phaseName, ${illuminationPercent.toInt()}% illuminated"
    } else {
        "Moon phase"
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.toPx() / 2f
            val center = Offset(radius, radius)
            
            // Background disk (unlit)
            drawCircle(
                color = Color(0xFF2C2C2C),
                radius = radius,
                center = center
            )

            if (illuminationPercent == null || waxing == null) return@Canvas

            val illumination = (illuminationPercent / 100.0).coerceIn(0.0, 1.0)
            val moonColor = Color(0xFFF5F3CE) // Pale moon yellow

            // To represent the moon phases, we draw a half-circle for the primary lit side
            // and an ellipse over it to create the crescent or gibbous bulge.
            
            // Side depends on waxing (lit on right) vs waning (lit on left)
            val startAngle = if (waxing) 270f else 90f
            
            if (illumination <= 0.5) {
                // Crescent phases: Primary half is unlit. Draw a smaller lit portion.
                // Bulge width factor (1.0 at New Moon, 0.0 at Quarter)
                val widthFactor = (1.0 - illumination * 2.0).toFloat()
                
                // Draw lit half-circle
                drawArc(
                    color = moonColor,
                    startAngle = startAngle,
                    sweepAngle = 180f,
                    useCenter = true,
                    size = Size(radius * 2, radius * 2)
                )
                
                // Draw dark ellipse to carve the crescent
                drawOval(
                    color = Color(0xFF2C2C2C),
                    topLeft = Offset(radius - (radius * widthFactor), 0f),
                    size = Size(radius * 2 * widthFactor, radius * 2)
                )
            } else {
                // Gibbous phases: Primary half is fully lit.
                // Bulge width factor (0.0 at Quarter, 1.0 at Full)
                val widthFactor = ((illumination - 0.5) * 2.0).toFloat()
                
                // Draw lit half-circle
                drawArc(
                    color = moonColor,
                    startAngle = startAngle,
                    sweepAngle = 180f,
                    useCenter = true,
                    size = Size(radius * 2, radius * 2)
                )
                
                // Draw lit ellipse to create the gibbous bulge
                drawOval(
                    color = moonColor,
                    topLeft = Offset(radius - (radius * widthFactor), 0f),
                    size = Size(radius * 2 * widthFactor, radius * 2)
                )
            }
        }
    }
}
