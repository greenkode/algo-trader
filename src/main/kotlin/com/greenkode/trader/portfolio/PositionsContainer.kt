package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.math.BigDecimal
import java.time.LocalDateTime

open class PositionsContainer(initialBalance: BigDecimal, currentDate: LocalDateTime) {

    private val positionsHistory = mutableListOf<Positions>()
    private var currentPositions = Positions(currentDate, initialBalance, mapOf())

    fun getCurrentPositions(): Positions {
        return currentPositions
    }

    fun newRecord(timestamp: LocalDateTime, closePrices: Map<Symbol, BigDecimal>) {
        val newPosition = Positions(timestamp, currentPositions.getCash(), closePrices)
        positionsHistory.add(this.currentPositions)
        this.currentPositions = newPosition
    }

    fun addPosition(position: Position) {
        currentPositions.addPosition(position)
    }

    fun getQuantityForSymbol(symbol: Symbol): BigDecimal {
        return positionsHistory[positionsHistory.lastIndex].getQuantity(symbol)
    }

    fun getHistoricalPositions(): List<Positions> {
        return positionsHistory
    }

    fun getCurrentValue(): BigDecimal {
        return positionsHistory[positionsHistory.lastIndex].getCurrentValue()
    }
}