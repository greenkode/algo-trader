package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.math.BigDecimal
import java.time.LocalDateTime

open class Position(
    open var timestamp: LocalDateTime? = null,
    open val positions: MutableMap<Symbol, BigDecimal>
)
