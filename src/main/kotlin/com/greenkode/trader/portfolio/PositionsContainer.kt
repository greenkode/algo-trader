package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.math.BigDecimal
import java.time.LocalDateTime

open class PositionsContainer(
    symbols: List<Symbol>
) {

    private val records = mutableListOf<Positions>()
    private var currentPositions: Positions =
        Positions(null, symbols.associateBy({ it }, { BigDecimal.ZERO }).toMutableMap())

    fun getCurrentPositions(): Positions {
        return currentPositions
    }

    fun newRecord(timestamp: LocalDateTime, currentPositions: Positions) {
        this.currentPositions = Positions(timestamp, currentPositions.positions.toMutableMap())
        records.add(this.currentPositions)
    }

    fun updateQuantity(symbol: Symbol, quantity: BigDecimal) {
        currentPositions.positions[symbol] = quantity
    }

    fun getQuantityForSymbol(symbol: Symbol): BigDecimal {
        return currentPositions.positions.getOrDefault(symbol, BigDecimal.ZERO)
    }
}