package com.greenkode.trader.strategy

import tech.tablesaw.aggregate.AggregateFunctions
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.Table

class MomentumRebalanceStrategy {

    fun volatility(numbers: List<Double>) {
        val prices: DoubleColumn = DoubleColumn.create("prices", numbers).pctChange()
        val table = Table.create(prices).summarize(prices, AggregateFunctions.stdDev).apply()
        print(table)
    }
}