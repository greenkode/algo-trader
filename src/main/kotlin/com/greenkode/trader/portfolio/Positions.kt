package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.math.BigDecimal
import java.time.LocalDateTime

class Positions(val timestamp: LocalDateTime?, val positions: MutableMap<Symbol, BigDecimal>) {

    fun updateQuantity(symbol: Symbol, quantity: BigDecimal) {
        positions[symbol] = quantity
    }
}