package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import tech.tablesaw.aggregate.AggregateFunctions
import tech.tablesaw.api.ColumnType
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.Table
import java.math.BigDecimal

class RiskManager {

    fun allocateWeights(series: Table, ranking: Map<Symbol, BigDecimal>): Map<Symbol, BigDecimal> {

        val weights = mutableMapOf<Symbol, BigDecimal>()

        ranking.forEach { (k, v) ->
            if (v < BigDecimal.valueOf(40)) {
                weights[k] = BigDecimal.ZERO
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
            weights[Symbol(name)] = BigDecimal.valueOf((it as DoubleColumn).divide(sum).get(0))
        }

        return weights
    }

    private fun volatility(table: Table): Table {
        val prices = Table.create()
        table.columnsOfType(ColumnType.DOUBLE).forEach { column ->
            prices.addColumns((column as DoubleColumn).pctChange().setName(column.name()))
        }
        return prices.summarize(prices.columnNames(), AggregateFunctions.stdDev).apply()
    }
}
