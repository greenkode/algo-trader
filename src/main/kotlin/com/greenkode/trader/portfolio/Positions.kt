package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import sumByBigDecimal
import java.math.BigDecimal
import java.time.LocalDateTime

class Positions(
    val timestamp: LocalDateTime,
    initialBalance: BigDecimal,
    private val closePrices: Map<Symbol, BigDecimal>
) {

    private val positions = mutableListOf<Position>()
    private var cash: BigDecimal = initialBalance

    fun getCommissions(): BigDecimal {
        return positions.map { position -> position.commission }.sumByBigDecimal { it }
    }

    fun getCurrentValue(): BigDecimal {
        return positions.map { closePrices.getOrElse(it.symbol) { it.price } * it.quantity }
            .sumByBigDecimal { it } + cash
    }

    fun addPosition(position: Position) {
        cash -= position.price * position.quantity + position.commission
        this.positions.add(position)
    }

    fun getQuantity(symbol: Symbol): BigDecimal {
        val filtered = positions.filter { position -> position.symbol == symbol }
        return if (filtered.size == 1) filtered.first().quantity else BigDecimal.ZERO
    }

    fun getCash(): BigDecimal {
        return cash
    }
}