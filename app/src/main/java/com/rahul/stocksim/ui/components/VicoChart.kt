package com.rahul.stocksim.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.rahul.stocksim.data.StockPricePoint
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VicoLineChart(
    history: List<StockPricePoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    
    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(history.map { it.price })
                }
            }
        }
    }

    if (history.isEmpty()) return

    val marker = rememberCartesianMarker()

    val rangeProvider = remember {
        object : CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                val delta = maxY - minY
                return if (delta > 0) minY - delta * 0.1 else minY * 0.9
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                val delta = maxY - minY
                return if (delta > 0) maxY + delta * 0.1 else maxY * 1.1
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(lineColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(
                            fill(
                                DynamicShader.verticalGradient(
                                    arrayOf(lineColor.copy(alpha = 0.4f), lineColor.copy(alpha = 0f))
                                )
                            )
                        ),
                        thickness = 2.dp,
                        pointConnector = LineCartesianLayer.PointConnector.cubic()
                    )
                ),
                rangeProvider = rangeProvider
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = rememberTextComponent(
                    color = Color.Gray,
                    textSize = 10.sp,
                    padding = Dimensions(4f, 4f, 4f, 4f)
                ),
                line = null,
                tick = null,
                guideline = null,
                valueFormatter = { _, value, _ ->
                    val index = value.toInt().coerceIn(0, history.size - 1)
                    val timestamp = history[index].timestamp * 1000
                    val date = Date(timestamp)
                    val format = if (history.size < 100) "HH:mm" else "MMM dd"
                    SimpleDateFormat(format, Locale.getDefault()).format(date)
                }
            ),
            endAxis = VerticalAxis.rememberEnd(
                label = rememberTextComponent(
                    color = Color.Gray, 
                    textSize = 10.sp,
                    padding = Dimensions(4f, 4f, 4f, 4f)
                ),
                line = null,
                tick = null,
                guideline = rememberLineComponent(
                    fill = fill(Color.White.copy(alpha = 0.1f)),
                    thickness = 1.dp
                ),
                valueFormatter = { _, value, _ -> String.format(Locale.US, "$%.2f", value) }
            ),
            marker = marker
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false)
    )
}

@Composable
fun rememberCartesianMarker(): DefaultCartesianMarker {
    val labelBackground = rememberShapeComponent(
        fill = fill(Color(0xFF252525)),
        shape = CorneredShape.rounded(4f),
    )
    val label = rememberTextComponent(
        color = Color.White,
        background = labelBackground,
        padding = Dimensions(8f, 4f, 8f, 4f),
        textSize = 12.sp,
        lineCount = 1
    )
    val indicator = rememberShapeComponent(fill = fill(Color.White), shape = CorneredShape.Pill)
    val guideline = rememberLineComponent(
        fill = fill(Color.White.copy(alpha = 0.5f)),
        thickness = 1.dp
    )
    
    return remember(label, indicator, guideline) {
        DefaultCartesianMarker(
            label = label,
            indicator = { indicator },
            indicatorSizeDp = 6f,
            guideline = guideline
        )
    }
}
