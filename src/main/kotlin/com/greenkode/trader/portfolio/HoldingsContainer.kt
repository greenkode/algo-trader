package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.DATA_COLUMN_CLOSE
import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.domain.ZERO
import tech.tablesaw.api.Table
import java.time.LocalDateTime
import java.util.*


data class HoldingsContainer(
    private val initialCapital: Double,
    private val symbols: List<Symbol>
) {

    private val records = mutableListOf<Holdings>()
    private var currentHoldings =
        Holdings(null, symbols.associateBy({ it }, { Double.ZERO }).toMutableMap(), initialCapital)

    init {
        records.add(currentHoldings)
    }

    fun addHoldings(holdings: Holdings) {
        records.add(holdings)
    }

    fun getHoldingsHistory(): List<Holdings> {
        return Collections.unmodifiableList(records)
    }

    fun newRecord(timestamp: LocalDateTime, positions: Positions, bars: Map<Symbol, Table>) {
        val values = mutableMapOf<Symbol, Double>()
        symbols.forEach { symbol ->
            val marketValue = positions.positions[symbol]!! * getLatestClose(bars.getOrElse(symbol) { Table.create() })
            values[symbol] = marketValue
        }
        this.currentHoldings = Holdings(timestamp, values, currentHoldings.getTotal())
        records.add(currentHoldings)
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

    fun getHoldingForSymbol(symbol: Symbol): Double {
        return currentHoldings.holdings.getOrDefault(symbol, 0.0)
    }

    fun getCurrentHoldings(): Holdings {
        return currentHoldings
    }
}