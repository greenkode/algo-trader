package com.greenkode.trader.analysis

import com.greenkode.trader.domain.*
import com.greenkode.trader.logger.LoggerDelegate
import com.greenkode.trader.portfolio.Positions
import tech.tablesaw.api.DateTimeColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.Table
import kotlin.math.max
import kotlin.math.sqrt

class Performance {

    private val logger by LoggerDelegate()

    private val equityCurve: Table

    init {

        val columns = listOf(
            DateTimeColumn.create(DATA_COLUMN_TIMESTAMP),
            DoubleColumn.create(EQUITY_CURVE_CASH),
            DoubleColumn.create(EQUITY_CURVE_COMMISSION),
            DoubleColumn.create(EQUITY_CURVE_TOTAL)
        )

        equityCurve = Table.create("Equity Curve", columns)
    }

    private fun createSharpeRatio(equityCurve: DoubleColumn, periods: Int = 252): Double {
        return sqrt(periods.toDouble()) * equityCurve.mean() / equityCurve.standardDeviation()
    }

    private fun createDrawDowns(equityCurve: DoubleColumn): Pair<Double, Double> {

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


    fun createEquityCurve(positions: List<Positions>): Table {

        positions.forEach { holding ->
            equityCurve.dateTimeColumn(DATA_COLUMN_TIMESTAMP).append(holding.timestamp)
            equityCurve.doubleColumn(EQUITY_CURVE_CASH).append(holding.getCash())
            equityCurve.doubleColumn(EQUITY_CURVE_COMMISSION).append(holding.getCommissions())
            equityCurve.doubleColumn(EQUITY_CURVE_TOTAL).append(holding.getCurrentValue())
        }
        val returns = equityCurve.doubleColumn(EQUITY_CURVE_TOTAL).pctChange().setName(EQUITY_CURVE_RETURNS)
        equityCurve.addColumns(returns)
        equityCurve.addColumns(returns.add(1).cumProd().setName(EQUITY_CURVE_CURVE))

        logger.debug(equityCurve.print())

        return equityCurve
    }

    fun printSummaryStats() {

        val totalReturn = equityCurve.last().getDouble(EQUITY_CURVE_TOTAL)
        val returns = equityCurve.doubleColumn(EQUITY_CURVE_RETURNS)
        val pnl = equityCurve.doubleColumn(EQUITY_CURVE_CURVE)

        val sharpeRatio = createSharpeRatio(returns, 365)
        val drawDowns = createDrawDowns(pnl)

        logger.info(
            "Total Return=${((totalReturn - 1.0) * 100.0)}\n" +
                    "Sharpe Ratio=${sharpeRatio}\n" +
                    "Max DrawDown=${(drawDowns.first * 100.0)}\n" +
                    "DrawDown Duration=${drawDowns.second}"
        )
    }
}