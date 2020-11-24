package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.time.LocalDateTime

open class PositionsContainer(
    private val symbols: List<Symbol>
) {

    private val records = mutableListOf<Positions>()
    private lateinit var currentPositions: Positions

    fun getCurrentPositions(): Positions {
        return currentPositions
    }

    fun newRecord(timestamp: LocalDateTime) {
        currentPositions = Positions(timestamp, symbols)
        records.add(currentPositions)
    }

    fun updateQuantity(symbol: Symbol, quantity: Double) {
        currentPositions.positions[symbol] = quantity
    }

    fun getCurrentQuantity(symbol: Symbol): Double {
        return currentPositions.positions.getOrDefault(symbol, 0.0)
    }
}