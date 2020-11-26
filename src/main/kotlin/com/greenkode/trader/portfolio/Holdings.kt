package com.greenkode.trader.portfolio;

import com.greenkode.trader.domain.Symbol;
import java.math.BigDecimal

import java.time.LocalDateTime;

class Holdings(
    val timestamp: LocalDateTime? = null,
    val holdings: MutableMap<Symbol, BigDecimal>,
    initialCapital: BigDecimal
) {

    private var total = initialCapital
    private var totalCommission = BigDecimal.ZERO
    private var cash = initialCapital

    fun setHoldingAmount(symbol: Symbol, cost: BigDecimal, commission: BigDecimal) {
        holdings[symbol] = cost - commission
        total -= (cost + commission)
        totalCommission += commission
        cash -= (cost + commission)
    }

    fun getCommissions(): BigDecimal {
        return totalCommission
    }

    fun getCash(): BigDecimal {
        return total
    }

    fun getTotal(): BigDecimal {
        return total
    }
}
