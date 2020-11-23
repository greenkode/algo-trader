package com.greenkode.trader.portfolio;

import com.greenkode.trader.domain.Symbol;
import com.greenkode.trader.domain.ZERO

import java.time.LocalDateTime;

class Holdings(val timestamp: LocalDateTime? = null, symbols: List<Symbol>) {

    private val holdings: MutableMap<Symbol, Double> = symbols.associateBy({ it }, { Double.ZERO }).toMutableMap()
    private var total = 0.0

    fun setHoldingAmount(symbol: Symbol, amount: Double) {
        holdings[symbol] = amount
        total += amount
    }

    fun getCommissions(): Double {
        TODO("Not yet implemented")
    }

    fun getCash(): Double {
        TODO("Not yet implemented")
    }

    fun getTotal(): Double {
        TODO("Not yet implemented")
    }


}
