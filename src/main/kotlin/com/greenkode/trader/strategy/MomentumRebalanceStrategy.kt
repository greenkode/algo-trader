package com.greenkode.trader.strategy

import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.*
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.RebalanceEvent
import com.greenkode.trader.event.SignalEvent
import org.apache.commons.math3.stat.regression.SimpleRegression
import tech.tablesaw.api.*
import java.time.LocalDateTime
import java.util.*
import kotlin.math.exp
import kotlin.math.pow

class MomentumRebalanceStrategy(val dataHandler: DataHandler, val events: Queue<Event>) : Strategy() {

    var bought = mutableMapOf<Symbol, Boolean>()
    lateinit var currentDate: LocalDateTime

    init {
        calculateInitialBought()
    }

    private fun calculateInitialBought() {
        dataHandler.symbols.forEach { symbol ->
            bought[symbol] = false
        }
    }

    override fun calculateSignals(event: Event) {
        if (event.type != EventTypeEnum.MARKET) return

        val window = 20
        val series = getBars(window)
        if (series.count() < window) return

        currentDate = series.last().getDateTime(DATA_COLUMN_TIMESTAMP)

        val rankingTable = momentumScore(series)

        events.add(RebalanceEvent(rankingTable, series))

        dataHandler.symbols.forEach { symbol ->
            if (rankingTable.getOrDefault(symbol, 0.0) > 40)
                events.add(SignalEvent(symbol, currentDate, OrderDirection.BUY, 1.0))
            else if (rankingTable.getOrDefault(symbol, 0.0) != 0.0)
                events.add(SignalEvent(symbol, currentDate, OrderDirection.EXIT, 1.0))
        }
    }

    private fun getBars(window: Int): Table {

        val result = Table.create()
        var addedDate = false
        dataHandler.symbols.forEach { symbol ->
            val table = dataHandler.getLatestBars(symbol, window = window)
            if (!table.isEmpty && table.count() == window) {
                if (!addedDate) {
                    result.addColumns(table.dateTimeColumn(DATA_COLUMN_TIMESTAMP))
                    addedDate = true
                }
                result.addColumns(table.doubleColumn(DATA_COLUMN_CLOSE).setName(symbol.name))
            }
        }
        return result
    }

    private fun momentumScore(table: Table): Map<Symbol, Double> {
        val result = mutableMapOf<Symbol, Double>()
        val logs = Table.create()
        table.columnsOfType(ColumnType.DOUBLE).forEach {
            logs.addColumns((it as DoubleColumn).log10().setName(it.name()))
        }
        logs.columns().forEach { column ->
            val reg = SimpleRegression(true)
            reg.addData(arrayOf(DoubleArray(table.count()) { i -> i + 1.0 }, (column as DoubleColumn).asDoubleArray()))
            val annualizedSlope = (exp(reg.slope).pow(365.0) - 1) * 100
            result[Symbol(column.name())] = annualizedSlope * (reg.rSquare)
        }

        return result.toList().sortedBy { it.second }.reversed().toMap()
    }
}