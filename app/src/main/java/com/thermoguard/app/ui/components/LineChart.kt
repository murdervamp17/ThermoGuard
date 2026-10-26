package com.thermoguard.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermoguard.app.data.formatTemp
import com.thermoguard.app.ui.theme.Critical
import com.thermoguard.app.ui.theme.TextDim
import com.thermoguard.app.ui.theme.Track

/** Time-scaled area chart. Points are (epochMillis, °C). */
@Composable
fun LineChart(
    points: List<Pair<Long, Float>>,
    lineColor: Color,
    fahrenheit: Boolean,
    modifier: Modifier = Modifier,
    thresholdC: Float? = null,
) {
    if (points.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Collecting data…", color = TextDim)
        }
        return
    }
    val values = points.map { it.second }
    val rawMin = values.min()
    val rawMax = values.max()
    val pad = maxOf(1f, (rawMax - rawMin) * 0.15f)
    val lo = rawMin - pad
    val hi = rawMax + pad
    val x0 = points.first().first
    val spanX = (points.last().first - x0).coerceAtLeast(1L).toFloat()

    Box(modifier) {
        Canvas(Modifier.fillMaxSize().padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 18.dp)) {
            fun toOffset(p: Pair<Long, Float>) = Offset(
                x = (p.first - x0).toFloat() / spanX * size.width,
                y = size.height - (p.second - lo) / (hi - lo) * size.height,
            )
            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(Track, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val line = Path()
            points.forEachIndexed { i, p ->
                val o = toOffset(p)
                if (i == 0) line.moveTo(o.x, o.y) else line.lineTo(o.x, o.y)
            }
            val first = toOffset(points.first())
            val last = toOffset(points.last())
            val fill = Path().apply {
                addPath(line)
                lineTo(last.x, size.height)
                lineTo(first.x, size.height)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)))
            drawPath(line, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            if (thresholdC != null && thresholdC in lo..hi) {
                val y = size.height - (thresholdC - lo) / (hi - lo) * size.height
                drawLine(
                    Critical, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                )
            }
            drawCircle(lineColor.copy(alpha = 0.3f), radius = 9.dp.toPx(), center = last)
            drawCircle(Color.White, radius = 4.dp.toPx(), center = last)
        }
        Text(formatTemp(rawMax, fahrenheit), color = TextDim, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopStart))
        Text(formatTemp(rawMin, fahrenheit), color = TextDim, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart))
    }
}
