package com.greenkode.trader.strategy

import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.*
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.RebalanceEvent
import com.greenkode.trader.event.SignalEvent
import com.greenkode.trader.portfolio.Portfolio
import com.greenkode.trader.portfolio.RiskManager
import org.apache.commons.math3.stat.regression.SimpleRegression
import tech.tablesaw.api.ColumnType
import tech.tablesaw.api.DateTimeColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class MomentumRebalanceStrategy(
    private val dataHandler: DataHandler,
    private val events: Queue<Event>,
    private val riskManager: RiskManager,
    private val portfolio: Portfolio
) :
    Strategy() {

    private lateinit var currentDate: LocalDateTime

    private val window = 20
    private val portfolioSize = 10
    private val minMomentum = BigDecimal.valueOf(500)

    override fun calculateSignals(event: Event) {
        if (event.type != EventTypeEnum.MARKET) return

        val series = getBars(window)
        if (series.count() < window) return

        currentDate = series.last().getDateTime(DATA_COLUMN_TIMESTAMP)

        val rankingTable = momentumScore(series)

        val weights = riskManager.allocateWeights(series, rankingTable)

        sellFailedPositions(weights, rankingTable)

        adjustKeptPositions(weights, rankingTable)
    }

    private fun sellFailedPositions(
        weights: Map<Symbol, BigDecimal>,
        rankingTable: Map<Symbol, BigDecimal>
    ) {
        weights.keys.filterIndexed { index, _ -> index < portfolioSize }.forEach { symbol ->
            if (rankingTable.getOrDefault(symbol, BigDecimal.ZERO) < minMomentum && portfolio.getCurrentPositions()
                    .getQuantity(symbol) > BigDecimal.ZERO
            )
                events.add(weights[symbol]?.let { SignalEvent(symbol, currentDate, OrderDirection.EXIT, it) })
        }
    }

    private fun adjustKeptPositions(
        weights: Map<Symbol, BigDecimal>,
        rankingTable: Map<Symbol, BigDecimal>
    ) {
        val signals = mutableListOf<SignalEvent>()
        weights.keys.filterIndexed { index, _ -> index < portfolioSize }.forEach { symbol ->
            if (rankingTable.getOrDefault(symbol, BigDecimal.ZERO) >= minMomentum)
                signals.add(
                    SignalEvent(
                        symbol,
                        currentDate,
                        OrderDirection.LONG,
                        weights.getOrElse(symbol) { BigDecimal.ZERO })
                )
        }
        events.add(RebalanceEvent(signals))
    }

    private fun getBars(window: Int): Table {

        val result = Table.create()
        dataHandler.symbols.forEach { symbol ->
            val table = dataHandler.getLatestBars(symbol, window = window)
            if (!table.isEmpty && table.count() == window) {
                addDateColumn(result, table.dateTimeColumn(DATA_COLUMN_TIMESTAMP))
                result.addColumns(table.doubleColumn(DATA_COLUMN_CLOSE).setName(symbol.name))
            }
        }
        return result
    }

    private fun addDateColumn(table: Table, dateTimeColumn: DateTimeColumn) {
        if (!table.columnNames().contains(dateTimeColumn.name()))
            table.addColumns(dateTimeColumn)
    }

    private fun momentumScore(table: Table): Map<Symbol, BigDecimal> {
        val result = mutableMapOf<Symbol, BigDecimal>()
        val logs = Table.create()
        table.columnsOfType(ColumnType.DOUBLE).forEach {
            logs.addColumns((it as DoubleColumn).log10().setName(it.name()))
        }
        logs.columns().forEach { column ->
            val reg = SimpleRegression(true)
            reg.addData(arrayOf(DoubleArray(table.count()) { i -> i + 1.0 }, (column as DoubleColumn).asDoubleArray()))
            val annualizedSlope = (reg.slope) * 100
            result[Symbol(column.name())] =
                BigDecimal.valueOf(if (annualizedSlope.isFinite()) annualizedSlope * (reg.rSquare) else 0.0)
        }

        return result.toList().sortedBy { it.second }.reversed().toMap()
    }
}