package com.greenkode.trader.analysis

import tech.tablesaw.api.DoubleColumn
import kotlin.math.max

fun createSharpeRatio(equityCurve: DoubleColumn, periods: Int = 252): Double {
    return Math.sqrt(periods.toDouble()) * equityCurve.mean() / equityCurve.standardDeviation()
}

fun createDrawdowns(equityCurve: DoubleColumn): Pair<Double, Double> {

    val highWaterMark = mutableListOf(0.0)
    val drawDown = mutableMapOf<Int, Double>()
    val duration = mutableMapOf<Int, Double>()

    equityCurve.forEachIndexed { index, _ ->
        if (index > 0) {
            val currentHighWaterMark = max(highWaterMark[index - 1], equityCurve.get(index))
            highWaterMark.add(currentHighWaterMark)
            drawDown[index] = highWaterMark[index] - equityCurve.get(index)
            duration[index] = if (drawDown[index] == 0.0) 0.0 else duration[index - 1]!! + 1
        }
    }
    return Pair(drawDown.values.maxOrNull()!!, duration.values.maxOrNull()!!)
}