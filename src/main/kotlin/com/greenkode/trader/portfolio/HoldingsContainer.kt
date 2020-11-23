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


    private var commissions = 0.0
    private var total: Double = initialCapital

    private val records = mutableListOf<Holdings>()
    private val currentHoldings = Holdings(null, symbols)

    fun addHoldings(holdings: Holdings) {
        records.add(holdings)
    }

    fun getHoldingsHistory(): List<Holdings> {
        return Collections.unmodifiableList(records)
    }

    fun newRecord(timestamp: LocalDateTime, positions: Positions, bars: Map<Symbol, Table>) {
        val holdings = Holdings(timestamp, symbols)
        symbols.forEach { symbol ->
            val marketValue =
                positions.positions[symbol]!! * getLatestClose(bars.getOrElse(symbol) { Table.create() })
            holdings.setHoldingAmount(symbol, marketValue)
        }
        records.add(holdings)
    }

    fun updateHoldings(timestamp: LocalDateTime, cost: Double, positions: Positions) {
        TODO("Implement logic")
//        timestamp = fillEvent.timestamp,
//        positions = currentHoldingsContainer.positions,
//        commission = currentHoldingsContainer.commission + fillEvent.calculateCommission(),
//        cash = currentHoldingsContainer.cash - (cost + fillEvent.calculateCommission()),
//        total = currentHoldingsContainer.total - (cost + fillEvent.calculateCommission())
    }

    private fun getLatestClose(bars: Table): Double {
        if (!bars.isEmpty)
            return bars.first().getDouble(DATA_COLUMN_CLOSE)
        return 0.0
    }

    fun getCurrentTotal(): Double {
        return total;
    }
}