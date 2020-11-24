package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.domain.ZERO
import java.time.LocalDateTime

class Positions(val timestamp: LocalDateTime, symbols: List<Symbol>) {

    val positions: MutableMap<Symbol, Double> = symbols.associateBy({ it }, { Double.ZERO }).toMutableMap()
}