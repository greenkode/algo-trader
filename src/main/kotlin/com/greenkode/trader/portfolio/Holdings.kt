package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.time.LocalDateTime


data class Holdings(
    override var timestamp: LocalDateTime? = null,
    var cash: Double,
    val commission: Double,
    var total: Double,
    override val positions: MutableMap<Symbol, Double>
) : Positions(timestamp = timestamp, positions = positions)