package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import java.math.BigDecimal

data class Position(val symbol: Symbol, val quantity: BigDecimal, val price: BigDecimal, val commission: BigDecimal)
