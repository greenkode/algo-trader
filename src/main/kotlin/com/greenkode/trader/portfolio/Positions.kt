package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.time.LocalDateTime

open class Positions(
    open var timestamp: LocalDateTime? = null,
    open val positions: MutableMap<Symbol, Double>
)
