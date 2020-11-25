package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.time.LocalDateTime

class Positions(val timestamp: LocalDateTime?, val positions: MutableMap<Symbol, Double>) {

    fun updateQuantity(symbol: Symbol, quantity: Double) {
        positions[symbol] = quantity
    }
}