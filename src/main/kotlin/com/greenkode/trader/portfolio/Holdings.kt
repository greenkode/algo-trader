package com.greenkode.trader.portfolio;

import com.greenkode.trader.domain.Symbol;

import java.time.LocalDateTime;

class Holdings(
    val timestamp: LocalDateTime? = null,
    val holdings: MutableMap<Symbol, Double>,
    initialCapital: Double
) {

    private var total = initialCapital
    private var totalCommission = 0.0
    private var cash = initialCapital

    fun setHoldingAmount(symbol: Symbol, cost: Double, commission: Double) {
        holdings[symbol] = cost - commission
        total -= (cost + commission)
        totalCommission += commission
        cash -= (cost + commission)
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
