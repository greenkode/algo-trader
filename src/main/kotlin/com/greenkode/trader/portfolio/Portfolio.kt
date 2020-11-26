package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.event.Event

abstract class Portfolio {

    abstract fun updateTimeIndex(event: Event)

    abstract fun updateFill(event: Event)

    abstract fun updateSignal(event: Event)

    abstract fun getHistoricalHoldings(): List<Holdings>

    abstract fun getCurrentPositions(): Map<Symbol, Double>

    abstract fun getCurrentHoldings(): Map<Symbol, Double>
}