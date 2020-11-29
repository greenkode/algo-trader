package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol;
import java.math.BigDecimal

import java.time.LocalDateTime;

class Holdings(
    val timestamp: LocalDateTime? = null,
    val holdings: MutableMap<Symbol, BigDecimal>,
    cash: BigDecimal
) {

    private var totalCommission = BigDecimal.ZERO
    private var cash = cash

    fun setHoldingAmount(symbol: Symbol, cost: BigDecimal, commission: BigDecimal) {
        holdings[symbol] = holdings.getOrDefault(symbol, BigDecimal.ZERO) + cost - commission
        totalCommission += commission
        cash -= cost + commission
    }

    fun getCommissions(): BigDecimal {
        return totalCommission
    }

    fun getCash(): BigDecimal {
        return cash
    }

    fun getTotal(): BigDecimal {
        return holdings.values.sumByBigDecimal { it } + cash
    }
}

fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
