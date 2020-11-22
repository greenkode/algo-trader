package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.RebalanceEvent
import com.greenkode.trader.event.SignalEvent
import tech.tablesaw.aggregate.AggregateFunctions
import tech.tablesaw.api.ColumnType
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.Table

class RiskManager {

    private val weights = mutableMapOf<Symbol, Double>()

    fun sizePosition(signal: SignalEvent): Double {
        return weights[signal.symbol] ?: Double.ZERO
    }

    fun allocateWeights(event: Event) {
        if (event.type != EventTypeEnum.REBALANCE)
            return
        val series = (event as RebalanceEvent).series

        event.rankingTable.forEach { (k, v) ->
            if (v < 40) {
                weights[k] = Double.ZERO
                series.removeColumns(k.name)
            }
        }

        val volatility = volatility(series)
        var sum = 0.0

        volatility.columnsOfType(ColumnType.DOUBLE).forEach {
            (it as DoubleColumn).set(0, 1 / it.get(0))
            sum += it.get(0)
        }

        volatility.columnsOfType(ColumnType.DOUBLE).forEach {
            val name = it.name().substring(it.name().indexOf('[') + 1, it.name().indexOf(']'))
            weights[Symbol(name)] = (it as DoubleColumn).divide(sum).get(0)
        }
    }

    private fun volatility(table: Table): Table {
        val prices = Table.create()
        table.columnsOfType(ColumnType.DOUBLE).forEach { column ->
            prices.addColumns((column as DoubleColumn).pctChange().setName(column.name()))
        }
        return prices.summarize(prices.columnNames(), AggregateFunctions.stdDev).apply()
    }
}
