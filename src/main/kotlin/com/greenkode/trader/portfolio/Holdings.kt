package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.time.LocalDateTime


data class Holdings(
    override var timestamp: LocalDateTime? = null,
    val cash: Double,
    val commission: Double,
    val total: Double,
    override val positions: MutableMap<Symbol, Double>
) : Positions(timestamp = timestamp, positions = positions) {

    private var _total: Double
        get() = field

    init {
        _total = total
    }

    fun addToTotal(value: Double) {
        _total += value
    }
}