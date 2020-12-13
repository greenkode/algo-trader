package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.DATA_COLUMN_CLOSE
import com.greenkode.trader.domain.Symbol
import tech.tablesaw.api.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*


data class HoldingsContainer(
    private val initialCapital: BigDecimal,
    val commissionPercentage: BigDecimal,
    private val symbols: List<Symbol>
) {

    private val records = mutableListOf<Holdings>()
    private var currentHoldings =
        Holdings(null, symbols.associateBy({ it }, { BigDecimal.ZERO }).toMutableMap(), initialCapital)

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
        val values = mutableMapOf<Symbol, BigDecimal>()
        symbols.forEach { symbol ->
            val marketValue = positions.positions[symbol]!! * getLatestClose(bars.getOrElse(symbol) { Table.create() })
            values[symbol] = marketValue
        }
        this.currentHoldings = Holdings(timestamp, values, currentHoldings.getCash())
        records.add(currentHoldings)
    }

    private fun getLatestClose(bars: Table): BigDecimal {
        if (!bars.isEmpty)
            return BigDecimal.valueOf(bars.first().getDouble(DATA_COLUMN_CLOSE))
        return BigDecimal.ZERO
    }

    fun updateHoldings(symbol: Symbol, cost: BigDecimal, commissions: BigDecimal) {
        currentHoldings.setHoldingAmount(symbol, cost, commissions)
    }

    fun getCurrentTotal(): BigDecimal {
        return currentHoldings.getTotal()
    }

    fun getCurrentHoldings(): Holdings {
        return currentHoldings
    }

    fun getCommissions(): BigDecimal {
        return currentHoldings.getCommissions()
    }

    fun getCurrentCash(): BigDecimal {
        return currentHoldings.getCash() * commissionPercentage
    }
}