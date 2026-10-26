package com.thermoguard.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermoguard.app.ui.theme.Cool
import com.thermoguard.app.ui.theme.Critical
import com.thermoguard.app.ui.theme.Frost
import com.thermoguard.app.ui.theme.Hot
import com.thermoguard.app.ui.theme.TextDim
import com.thermoguard.app.ui.theme.Track
import com.thermoguard.app.ui.theme.Warm
import kotlin.math.cos
import kotlin.math.sin

/** 270° arc gauge whose fill glides from frost-blue to ember-red. */
@Composable
fun TempGauge(
    celsius: Float?,
    display: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    minC: Float = 15f,
    maxC: Float = 55f,
    caption: String? = null,
) {
    val target = (((celsius ?: minC) - minC) / (maxC - minC)).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, animationSpec = tween(900), label = "gauge")

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val stroke = 22.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)

            drawArc(
                color = Track, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
            rotate(135f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Frost, 0.22f to Cool, 0.42f to Warm,
                        0.6f to Hot, 0.75f to Critical, 1f to Frost,
                    ),
                    startAngle = 0f, sweepAngle = 270f * progress, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            // Glowing marker at the tip of the arc
            val angle = Math.toRadians((135f + 270f * progress).toDouble())
            val r = arcSize.width / 2
            val tip = Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
            drawCircle(valueColor.copy(alpha = 0.35f), radius = stroke * 0.9f, center = tip)
            drawCircle(Color.White, radius = stroke * 0.32f, center = tip)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), color = TextDim, fontSize = 13.sp, letterSpacing = 2.sp)
            Text(display, color = valueColor, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            if (caption != null) Text(caption, color = TextDim, fontSize = 13.sp)
        }
    }
}
