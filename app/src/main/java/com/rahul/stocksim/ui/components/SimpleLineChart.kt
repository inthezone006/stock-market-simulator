package com.rahul.stocksim.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SimpleLineChart(
    historyData: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Green
) {
    if (historyData.size < 2) return
    val data = historyData.map { it.second }

    val minPriceActual = data.minOrNull() ?: 0.0
    val maxPriceActual = data.maxOrNull() ?: 1.0
    val rangeActual = maxPriceActual - minPriceActual
    
    // Add 10% padding to the range to keep the graph in the "middle"
    val padding = if (rangeActual == 0.0) maxPriceActual * 0.05 else rangeActual * 0.1
    val minPrice = minPriceActual - padding
    val maxPrice = maxPriceActual + padding
    val effectiveRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)

        val path = Path()
        val fillPath = Path()

        data.forEachIndexed { index, price ->
            val x = index * stepX
            val y = height - (((price - minPrice) / effectiveRange) * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if (index == data.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
