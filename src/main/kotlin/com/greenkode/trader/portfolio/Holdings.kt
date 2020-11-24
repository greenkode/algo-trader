package com.greenkode.trader.portfolio;

import com.greenkode.trader.domain.Symbol;
import com.greenkode.trader.domain.ZERO

import java.time.LocalDateTime;

class Holdings(val timestamp: LocalDateTime? = null, symbols: List<Symbol>, initialCapital: Double) {

    private val holdings: MutableMap<Symbol, Double> = symbols.associateBy({ it }, { Double.ZERO }).toMutableMap()
    private var total = initialCapital
    private var totalCommission = 0.0
    private var cash = initialCapital

    fun setHoldingAmount(symbol: Symbol, cost: Double, commissions: Double) {
        holdings[symbol] = cost - commissions
        total -= (cost)
        totalCommission += commissions
        cash -= (cost + commissions)
    }

    fun getCommissions(): Double {
        return totalCommission
    }

    fun getCash(): Double {
        return total
    }

    fun getTotal(): Double {
        return total
    }
}
