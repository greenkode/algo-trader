package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.math.BigDecimal
import java.time.LocalDateTime


data class Holdings(
    override var timestamp: LocalDateTime? = null,
    val cash: BigDecimal,
    val commission: Double,
    val total: BigDecimal,
    override val positions: MutableMap<Symbol, BigDecimal>
) : Positions(timestamp = timestamp, positions = positions) {

    private var _total: BigDecimal
        get() = field

    init {
        _total = total
    }

    fun addToTotal(value: BigDecimal) {
        _total = _total.add(value)
    }
}