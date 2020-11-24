package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.DATA_COLUMN_CLOSE
import com.greenkode.trader.domain.Symbol
import tech.tablesaw.api.Table
import java.time.LocalDateTime
import java.util.*


data class HoldingsContainer(
    private val initialCapital: Double,
    private val symbols: List<Symbol>
) {

    private val records = mutableListOf<Holdings>()
    private val currentHoldings = Holdings(null, symbols, initialCapital)

    fun addHoldings(holdings: Holdings) {
        records.add(holdings)
    }

    fun getHoldingsHistory(): List<Holdings> {
        return Collections.unmodifiableList(records)
    }

    fun newRecord(timestamp: LocalDateTime, positions: Positions, bars: Map<Symbol, Table>) {
        val holdings = Holdings(timestamp, symbols, initialCapital)
        symbols.forEach { symbol ->
            val marketValue =
                positions.positions[symbol]!! * getLatestClose(bars.getOrElse(symbol) { Table.create() })
            holdings.setHoldingAmount(symbol, marketValue, 0.0)
        }
        records.add(holdings)
    }

    private fun getLatestClose(bars: Table): Double {
        if (!bars.isEmpty)
            return bars.first().getDouble(DATA_COLUMN_CLOSE)
        return 0.0
    }

    fun updateHoldings(symbol: Symbol, cost: Double, commissions: Double) {
        currentHoldings.setHoldingAmount(symbol, cost, commissions)
    }

    fun getCurrentTotal(): Double {
        return currentHoldings.getTotal()
    }
}