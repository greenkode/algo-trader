package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.domain.ZERO
import java.time.LocalDateTime

open class PositionsContainer(
    symbols: List<Symbol>
) {

    private val records = mutableListOf<Positions>()
    private var currentPositions: Positions =
        Positions(null, symbols.associateBy({ it }, { Double.ZERO }).toMutableMap())

    fun getCurrentPositions(): Positions {
        return currentPositions
    }

    fun newRecord(timestamp: LocalDateTime, currentPositions: Positions) {
        this.currentPositions = Positions(timestamp, currentPositions.positions.toMutableMap())
        records.add(this.currentPositions)
    }

    fun updateQuantity(symbol: Symbol, quantity: Double) {
        currentPositions.positions[symbol] = quantity
    }

    fun getQuantityForSymbol(symbol: Symbol): Double {
        return currentPositions.positions.getOrDefault(symbol, 0.0)
    }
}