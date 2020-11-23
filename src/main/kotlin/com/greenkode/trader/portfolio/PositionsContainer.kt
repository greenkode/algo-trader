package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.time.LocalDateTime
import java.util.*

open class PositionsContainer(
    private val symbols: List<Symbol>
) {

    private val records = mutableListOf<Positions>()
    private lateinit var currentPositions: Positions

    fun getCurrentPositions(): Positions {
        return currentPositions
    }

    fun getPositionsHistory(): List<Positions> {
        return Collections.unmodifiableList(records);
    }

    fun newRecord(timestamp: LocalDateTime) {
        currentPositions = Positions(timestamp, symbols)
        records.add(currentPositions)
    }

    fun updateQuantity(symbol: Symbol, timestamp: LocalDateTime, quantity: Double) {
        if(currentPositions.timestamp != timestamp)
            newRecord(timestamp)
        currentPositions.positions[symbol] = quantity
    }

    fun getCurrentQuantity(): Double {
        TODO("Not yet implemented")
    }
}